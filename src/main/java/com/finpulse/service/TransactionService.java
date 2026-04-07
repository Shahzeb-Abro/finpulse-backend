package com.finpulse.service;

import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import com.finpulse.mappers.TransactionMapper;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.TransactionRepository;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final SecurityUtils securityUtils;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;
    private final LookupRepository lookupRepository;

    public ResponseEntity<ApiResponse<TransactionResponse>> addTransaction(TransactionRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();

        Lookup category = lookupRepository.findById(dto.getCategoryId()).orElseThrow();

        Transaction newTransaction = Transaction.builder()
                .user(loggedInUser)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .transactionType(dto.getTransactionType())
                .transactionDate(dto.getTransactionDate())
                .receiverName(dto.getReceiverName())
                .category(category)
                .activeFlag(Boolean.TRUE)
                .build();

        newTransaction = transactionRepository.save(newTransaction);

        return ResponseEntity.status(201).body(ApiResponse.success("Transaction created successfully", transactionMapper.mapDomainToResponseDto(newTransaction)));
    }

    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByCategory(Long categoryId) {
        User loggedInUser = securityUtils.getCurrentUser();

        Lookup category = lookupRepository.findById(categoryId).orElseThrow();

        if (category == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Category not found"));
        }

        List<Transaction> transactions = transactionRepository.findByCategoryAndUser(category, loggedInUser);

        if (transactions.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No transactions found for this category", List.of()));
        }

        List<TransactionResponse> mappedTransactions = transactions.stream()
                .map(transactionMapper::mapDomainToResponseDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", mappedTransactions));
    }

    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        User loggedInUser = securityUtils.getCurrentUser();

        List<Transaction> transactions = transactionRepository.findAllByUser(loggedInUser);

        if (transactions.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No transactions found", List.of()));
        }

        List<TransactionResponse> mappedTransactions = transactions.stream()
                .map(transactionMapper::mapDomainToResponseDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Transactions fetched successfully", mappedTransactions));
    }

    private ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(Long transactionId) {
        User loggedInUser = securityUtils.getCurrentUser();

        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, loggedInUser);

        if (transaction == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Transaction not found"));
        }

        return ResponseEntity.ok(ApiResponse.success("Transaction fetched successfully", transactionMapper.mapDomainToResponseDto(transaction)));
    }

}
