package com.finpulse.dto.request;

public record TransactionFilterRequest(
        int page,
        int size,
        String search,         // searches description
        String sortBy,         // "transactionDate" | "amount" | "description"
        String sortDir,        // "asc" | "desc"
        Long categoryId,       // optional — lookup id
        String transactionType // optional — "INCOME" | "EXPENSE"
) {}