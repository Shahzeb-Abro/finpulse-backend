package com.finpulse.specification;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a single parsed filter: field + operator + value.
 *
 * Parsed from query param format: "amount>:1000" or "transactionType::WITHDRAWAL"
 *
 * Supported operators:
 *   ::   equals          amount::2000
 *   !:   not equals      transactionType!:DEPOSIT
 *   >:   greater than    amount>:1000
 *   <:   less than       amount<:5000
 *   >=   greater/equal   amount>=1000
 *   <=   less/equal      amount<=5000
 */
@Getter
@AllArgsConstructor
public class SearchCriteria {

    private final String key;       // entity field name: "amount", "transactionType"
    private final String operator;  // ":", "!:", ">", "<", ">=", "<="
    private final String value;     // raw string value: "2000", "WITHDRAWAL"

    /**
     * Parses a single filter string into a SearchCriteria.
     *
     * Examples:
     *   "amount::2000"                → key=amount,     op=:,  value=2000
     *   "transactionType::WITHDRAWAL" → key=txnType,    op=:,  value=WITHDRAWAL
     *   "amount>:1000"                → key=amount,     op=>,  value=1000
     *   "amount<=5000"                → key=amount,     op=<=, value=5000
     *   "transactionType!:DEPOSIT"    → key=txnType,    op=!:, value=DEPOSIT
     *
     * Returns null if the format is invalid.
     */
    public static SearchCriteria parse(String filter) {
        if (filter == null || filter.isBlank()) return null;

        // Order matters — check multi-char operators first
        String[] operators = {">=", "<=", ">:", "<:", "!:", "::"};

        for (String op : operators) {
            int idx = filter.indexOf(op);
            if (idx > 0) {
                String key = filter.substring(0, idx).trim();
                // Normalize operator: ">:" → ">", "<:" → "<", "::" → ":"
                String normalizedOp = op.replace(":", "").isEmpty() ? ":" : op.replace(":", "");
                // Special case: "::" → ":", "!:" → "!"
                if (op.equals("::")) normalizedOp = ":";
                else if (op.equals("!:")) normalizedOp = "!";
                else if (op.equals(">:")) normalizedOp = ">";
                else if (op.equals("<:")) normalizedOp = "<";
                // >= and <= stay as-is

                String value = filter.substring(idx + op.length()).trim();
                if (key.isEmpty() || value.isEmpty()) return null;
                return new SearchCriteria(key, normalizedOp, value);
            }
        }
        return null;
    }
}