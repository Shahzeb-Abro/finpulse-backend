package com.finpulse.controller;

import com.finpulse.dto.request.RecurringBillRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.RecurringBillResponse;
import com.finpulse.service.RecurringBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/recurring-bills")
@RequiredArgsConstructor
public class RecurringBillController {
    private final RecurringBillService recurringBillService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PagedResponse<RecurringBillResponse>>> getAllRecurringBills( SearchDto searchDto) {
        return recurringBillService.getRecurringBills(searchDto);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<RecurringBillResponse>> createRecurringBill(@RequestBody RecurringBillRequest dto) {
        return recurringBillService.createRecurringBill(dto);
    }

    @PutMapping("/edit/{billId}")
    public ResponseEntity<ApiResponse<RecurringBillResponse>> editBill(@PathVariable("billId") Long billId, @RequestBody RecurringBillRequest dto) {
        return recurringBillService.editBill(billId, dto);
    }

    @DeleteMapping("/delete/{billId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecurringBill(@PathVariable("billId") Long billId) {
        return recurringBillService.deleteBill(billId);
    }
}
