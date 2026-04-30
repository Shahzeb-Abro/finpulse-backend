package com.finpulse.specification;

import com.finpulse.entity.User;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Builds JPA Specifications dynamically from search params.
 *
 * Handles two query params:
 *
 * 1. search — exact/comparison filters, comma-separated:
 *    ?search=amount>:1000,transactionType::WITHDRAWAL,categoryId::5
 *
 * 2. wildSearch — LIKE across ALL string fields on the entity:
 *    ?wildSearch=grocery
 *
 * Both are optional. When both are present, they're AND-ed together.
 *
 * USAGE:
 *    Specification<Transaction> spec = GenericSpecificationBuilder.build(
 *        Transaction.class, user, searchParam, wildSearchParam
 *    );
 *    Page<Transaction> page = transactionRepo.findAll(spec, pageable);
 *
 * TYPE COERCION:
 *    The builder inspects the JPA metamodel to determine each field's Java type,
 *    then converts the string value from the query param into the correct type:
 *      "2000"       → BigDecimal if field is BigDecimal
 *      "2000"       → Long if field is Long
 *      "WITHDRAWAL" → TransactionType.WITHDRAWAL if field is an Enum
 *      "2026-03-25" → Date if field is Date
 *      "true"       → Boolean if field is Boolean
 */
public class GenericSpecificationBuilder {

    private static final List<String> DATE_FORMATS = List.of(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd/MM/yyyy"
    );

    private GenericSpecificationBuilder() {}

    /**
     * Main entry point. Builds a complete Specification from search + wildSearch params.
     *
     * @param entityClass  The JPA entity class (Transaction.class, Budget.class, etc.)
     * @param user         Current authenticated user (always filtered)
     * @param search       Comma-separated exact filters: "amount>:1000,type::WITHDRAWAL"
     * @param wildSearch   Free-text LIKE search across all String fields
     */
    public static <T> Specification<T> build(
            Class<T> entityClass,
            User user,
            String search,
            String wildSearch
    ) {
        Specification<T> spec = Specification.where(userFilter(user));

        // Parse and apply each search filter
        if (search != null && !search.isBlank()) {
            List<SearchCriteria> criteria = parseCriteria(search);
            for (SearchCriteria c : criteria) {
                spec = spec.and(toSpecification(c));
            }
        }

        // Apply wildcard search across all string fields
        if (wildSearch != null && !wildSearch.isBlank()) {
            spec = spec.and(wildSearchSpec(wildSearch));
        }

        return spec;
    }

    /**
     * Always filter by the authenticated user.
     * Assumes every entity has a `user` field.
     */
    private static <T> Specification<T> userFilter(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    /**
     * Parses the comma-separated search string into individual SearchCriteria.
     * Input:  "amount>:1000,transactionType::WITHDRAWAL"
     * Output: [SearchCriteria(amount, >, 1000), SearchCriteria(transactionType, :, WITHDRAWAL)]
     */
    private static List<SearchCriteria> parseCriteria(String search) {
        List<SearchCriteria> criteria = new ArrayList<>();
        // Split by comma, but be careful — values themselves shouldn't contain commas
        String[] parts = search.split(",");
        for (String part : parts) {
            SearchCriteria sc = SearchCriteria.parse(part.trim());
            if (sc != null) {
                criteria.add(sc);
            }
        }
        return criteria;
    }

    /**
     * Converts a single SearchCriteria into a Specification predicate.
     * Inspects the entity's JPA metamodel to determine the field's Java type,
     * then coerces the string value and builds the correct predicate.
     */
    private static <T> Specification<T> toSpecification(SearchCriteria criteria) {
        return (root, query, cb) -> {
            String key = criteria.getKey();
            String op = criteria.getOperator();
            String rawValue = criteria.getValue();

            // Get the field's Java type from the JPA metamodel
            Path<?> path;
            Class<?> fieldType;
            try {
                // Handle nested paths like "category.id" → root.get("category").get("id")
                if (key.contains(".")) {
                    String[] segments = key.split("\\.");
                    Path<?> current = root;
                    for (String segment : segments) {
                        current = current.get(segment);
                    }
                    path = current;
                    fieldType = path.getJavaType();
                } else {
                    path = root.get(key);
                    fieldType = path.getJavaType();
                }
            } catch (IllegalArgumentException e) {
                // Field doesn't exist on this entity — skip silently
                return cb.conjunction();
            }

            // Coerce the raw string value to the field's actual type
            Object typedValue = coerceValue(rawValue, fieldType);
            if (typedValue == null) {
                return cb.conjunction(); // couldn't parse — skip
            }

            // Build the predicate based on the operator
            return buildPredicate(cb, path, op, typedValue, fieldType);
        };
    }

    /**
     * Builds the actual JPA predicate for a given operator.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildPredicate(
            CriteriaBuilder cb, Path<?> path, String op, Object value, Class<?> fieldType
    ) {
        switch (op) {
            case ":" -> {
                // Equals
                return cb.equal(path, value);
            }
            case "!" -> {
                // Not equals
                return cb.notEqual(path, value);
            }
            case ">" -> {
                if (Comparable.class.isAssignableFrom(fieldType)) {
                    return cb.greaterThan((Path<Comparable>) path, (Comparable) value);
                }
                return cb.conjunction();
            }
            case "<" -> {
                if (Comparable.class.isAssignableFrom(fieldType)) {
                    return cb.lessThan((Path<Comparable>) path, (Comparable) value);
                }
                return cb.conjunction();
            }
            case ">=" -> {
                if (Comparable.class.isAssignableFrom(fieldType)) {
                    return cb.greaterThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                }
                return cb.conjunction();
            }
            case "<=" -> {
                if (Comparable.class.isAssignableFrom(fieldType)) {
                    return cb.lessThanOrEqualTo((Path<Comparable>) path, (Comparable) value);
                }
                return cb.conjunction();
            }
            default -> {
                return cb.conjunction();
            }
        }
    }

    /**
     * LIKE search across ALL String fields on the entity.
     * Inspects the JPA metamodel to find every String attribute,
     * then builds: WHERE LOWER(field1) LIKE '%term%' OR LOWER(field2) LIKE '%term%' OR ...
     *
     * This means adding a new String field to your entity automatically
     * makes it searchable — zero code changes needed.
     */
    private static <T> Specification<T> wildSearchSpec(String searchTerm) {
        return (root, query, cb) -> {
            String pattern = "%" + searchTerm.trim().toLowerCase() + "%";

            // Get all declared attributes from the entity's metamodel
            EntityType<T> entityType = (EntityType<T>) root.getModel();
            List<Predicate> predicates = new ArrayList<>();

            for (Attribute<? super T, ?> attr : entityType.getDeclaredAttributes()) {
                // Only LIKE-search on String fields (skip numbers, dates, relationships)
                if (attr.getJavaType().equals(String.class)
                        && attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC) {
                    predicates.add(
                            cb.like(cb.lower(root.get(attr.getName())), pattern)
                    );
                }
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Converts a raw String value from the query param to the field's actual Java type.
     *
     * Supports: String, Long, Integer, BigDecimal, Double, Float,
     *           Boolean, Enum (any), Date (multiple formats).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object coerceValue(String rawValue, Class<?> targetType) {
        try {
            if (targetType.equals(String.class)) {
                return rawValue;
            }
            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                return Long.parseLong(rawValue);
            }
            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                return Integer.parseInt(rawValue);
            }
            if (targetType.equals(BigDecimal.class)) {
                return new BigDecimal(rawValue);
            }
            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                return Double.parseDouble(rawValue);
            }
            if (targetType.equals(Float.class) || targetType.equals(float.class)) {
                return Float.parseFloat(rawValue);
            }
            if (targetType.equals(Boolean.class) || targetType.equals(boolean.class)) {
                return Boolean.parseBoolean(rawValue);
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, rawValue.toUpperCase());
            }
            if (targetType.equals(LocalDate.class)) {
                return LocalDate.parse(rawValue); // parses "2026-04-28" natively
            }
            if (Date.class.isAssignableFrom(targetType)) {
                return parseDate(rawValue);
            }
        } catch (Exception e) {
            // Value doesn't match the expected type — return null to skip this filter
            return null;
        }
        return null;
    }

    /**
     * Tries multiple date formats to parse a date string.
     */
    private static Date parseDate(String raw) {
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(raw);
            } catch (ParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    public static Pageable buildPageable(int page, int pageSize, String sort) {
        // Clamp pageSize to prevent abuse
        int clampedSize = Math.min(Math.max(pageSize, 1), 100);

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0].trim();
        Sort.Direction direction = sortParts.length > 1
                && sortParts[1].trim().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, clampedSize, Sort.by(direction, sortField));
    }
}