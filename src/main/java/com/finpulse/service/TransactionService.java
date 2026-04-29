package com.finpulse.service;

import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.request.TransactionFilterRequest;
import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.PotResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import com.finpulse.mappers.TransactionMapper;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.TransactionRepository;
import com.finpulse.specification.GenericSpecificationBuilder;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    public ResponseEntity<ApiResponse<List<Lookup>>> getTransactionCategories() {
        List<Lookup> categories = lookupRepository.findByLookupType("TRANSACTION_CATEGORY");

        return ResponseEntity.ok(ApiResponse.success("Transaction categories fetched successfully", categories));
    }

    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(SearchDto searchDto) {

        Pageable pageable = GenericSpecificationBuilder.buildPageable(
                searchDto.getPage(),
                searchDto.getPageSize(),
                searchDto.getSort() != null ? searchDto.getSort() : "transactionDate,desc"
        );

        Specification<Transaction> spec = GenericSpecificationBuilder.build(
                Transaction.class,
                securityUtils.getCurrentUser(),
                searchDto.getSearch(),
                searchDto.getWildSearch()
        );

        Page<Transaction> resultPage = transactionRepository.findAll(spec, pageable);

        Page<TransactionResponse> pageResponse = resultPage.map(transactionMapper::mapDomainToResponseDto);

        return ResponseEntity.ok(ApiResponse.success("Pots fetched successfully", PagedResponse.from(pageResponse)));

    }

    public ResponseEntity<ApiResponse<TransactionResponse>> editTransaction(Long transactionId, TransactionRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();

        Transaction existingTransaction = transactionRepository.findByIdAndUser(transactionId, loggedInUser);

        if (existingTransaction == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No such transaction found"));
        }

        existingTransaction.setCategory(lookupRepository.findById(dto.getCategoryId()).orElseThrow());
        existingTransaction.setDescription(dto.getDescription());
        existingTransaction.setAmount(dto.getAmount());
        existingTransaction.setTransactionType(dto.getTransactionType());
        existingTransaction.setTransactionDate(dto.getTransactionDate());

        transactionRepository.save(existingTransaction);

        return ResponseEntity.ok().body(ApiResponse.success("Transaction updated successfully", transactionMapper.mapDomainToResponseDto(existingTransaction)));
    }

    public ResponseEntity<ApiResponse<Void>> deleteTransaction(Long transactionId) {
        User loggedInUser = securityUtils.getCurrentUser();
        Transaction transaction = transactionRepository.findByIdAndUser(transactionId, loggedInUser);
        if (transaction == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Transaction not found"));
        }
        transaction.setActiveFlag(Boolean.FALSE);
        transactionRepository.save(transaction);
        return ResponseEntity.ok().body(ApiResponse.success("Transaction deleted successfully"));
    }
}
