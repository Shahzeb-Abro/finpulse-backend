package com.finpulse.service;

import com.finpulse.dto.request.RecurringBillRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.RecurringBillResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.RecurringBill;
import com.finpulse.entity.User;
import com.finpulse.enums.BillPeriod;
import com.finpulse.enums.BillStatus;
import com.finpulse.mappers.RecurringBillMapper;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.RecurringBillRepository;
import com.finpulse.specification.GenericSpecificationBuilder;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class RecurringBillService {
    private final RecurringBillRepository recurringBillRepository;
    private final SecurityUtils securityUtils;
    private final RecurringBillMapper recurringBillMapper;
    private final LookupRepository lookupRepository;

    public ResponseEntity<ApiResponse<PagedResponse<RecurringBillResponse>>> getRecurringBills(SearchDto searchDto) {
        Pageable pageable = GenericSpecificationBuilder.buildPageable(searchDto.getPage(),
                searchDto.getPageSize(),
                searchDto.getSort() != null ? searchDto.getSort() : "id,desc");

        Specification<RecurringBill> spec = GenericSpecificationBuilder.build(
                RecurringBill.class,
                securityUtils.getCurrentUser(),
                searchDto.getSearch(),
                searchDto.getWildSearch()
        );

        Page<RecurringBill> resultPage = recurringBillRepository.findAll(spec, pageable);

        Page<RecurringBillResponse> pagedResponse = resultPage.map(recurringBillMapper::mapDomainToResponseDto);

        return ResponseEntity.ok(ApiResponse.success("Recurring bills fetched successfully", PagedResponse.from(pagedResponse)));
    }

    public ResponseEntity<ApiResponse<RecurringBillResponse>> createRecurringBill(RecurringBillRequest dto) {
        User user = securityUtils.getCurrentUser();

        LocalDate nextDueDate = computeNextDueDate(dto.getFrequency().name(), dto.getDueDate());

        Lookup category = lookupRepository.findById(dto.getCategoryId()).orElseThrow();

        RecurringBill bill = RecurringBill.builder()
                .user(user)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .category(category)
                .period(dto.getFrequency())
                .dueDate(dto.getDueDate())
                .nextPaymentDate(nextDueDate)
                .status(BillStatus.PENDING)
                .activeFlag(Boolean.TRUE)
                .build();

        bill = recurringBillRepository.save(bill);

        return ResponseEntity.status(201).body(ApiResponse.success("Recurring bill created successfully", recurringBillMapper.mapDomainToResponseDto(bill)));
    }

    public ResponseEntity<ApiResponse<RecurringBillResponse>> editBill(Long billId, RecurringBillRequest dto) {
        User user = securityUtils.getCurrentUser();

        RecurringBill bill = recurringBillRepository.findByIdAndUser(billId, user);

        if (bill == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Recurring bill with id " + billId + " not found"));
        }

        LocalDate nextDueDate = computeNextDueDate(dto.getFrequency().name(), dto.getDueDate());

        Lookup category = lookupRepository.findById(dto.getCategoryId()).orElseThrow();

        bill.setTitle(dto.getTitle());
                bill.setDescription(dto.getDescription());
                bill.setAmount(dto.getAmount());
                bill.setCategory(category);
                bill.setPeriod(dto.getFrequency());
                bill.setDueDate(dto.getDueDate());
                bill.setNextPaymentDate(nextDueDate);

        bill = recurringBillRepository.save(bill);
        return ResponseEntity.ok().body(ApiResponse.success("Bill updated successfully", recurringBillMapper.mapDomainToResponseDto(bill)));
    }

    public ResponseEntity<ApiResponse<Void>> deleteBill(Long billId) {
        User user = securityUtils.getCurrentUser();
        RecurringBill bill = recurringBillRepository.findByIdAndUser(billId, user);

        if (bill == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Recurring bill with id " + billId + " not found"));
        }

        bill.setActiveFlag(false);
        recurringBillRepository.save(bill);
        return ResponseEntity.status(204).body(ApiResponse.success("Bill deleted successfully"));
    }

    private LocalDate computeNextDueDate(String frequency, LocalDate dueDate) {
        LocalDate today = LocalDate.now();

        if (BillPeriod.MONTHLY.name().equals(frequency)) {
            LocalDate candidate = today.withDayOfMonth(dueDate.getDayOfMonth());
            if (candidate.isBefore(today)) {
                candidate = candidate.plusMonths(1);
            }
            return candidate;
        } else { // YEARLY
            LocalDate candidate = today
                    .withMonth(dueDate.getMonthValue())
                    .withDayOfMonth(dueDate.getDayOfMonth());
            return candidate.isBefore(today) ? candidate.plusYears(1) : candidate;
        }
    }
}
