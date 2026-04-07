package com.finpulse.controller;

import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        return transactionService.getAllTransactions();
    }

    @GetMapping("/get-by-category/{categoryId}")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByCategory(@PathVariable("categoryId") Long categoryId) {
        return transactionService.getTransactionsByCategory(categoryId);
    }

}
