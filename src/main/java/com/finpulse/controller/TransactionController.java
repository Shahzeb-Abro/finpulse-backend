package com.finpulse.controller;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.request.TransactionFilterRequest;
import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(@Valid @RequestBody TransactionRequest transactionRequest) {
        return transactionService.addTransaction(transactionRequest);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
           SearchDto dto
    ) {
        return transactionService.getTransactions(dto);
    }

    @GetMapping("/get-by-category/{categoryId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByCategory(@PathVariable("categoryId") Long categoryId) {
        return transactionService.getTransactionsByCategory(categoryId);
    }

    @GetMapping("/get-transaction-categories")
    public ResponseEntity<ApiResponse<List<Lookup>>> getTransactionCategories() {
        return transactionService.getTransactionCategories();
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> editTransaction(@PathVariable("transactionId") Long transactionId, @RequestBody TransactionRequest transactionRequest) {
        return transactionService.editTransaction(transactionId, transactionRequest);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable("transactionId") Long transactionId) {
        return transactionService.deleteTransaction(transactionId);
    }

}
