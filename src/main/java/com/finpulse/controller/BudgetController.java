package com.finpulse.controller;

import com.finpulse.dto.request.CreateBudgetRequestDto;
import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.*;
import com.finpulse.dto.response.budgetSummaryGraph.BudgetSummaryGraph;
import com.finpulse.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAllBudgets() {
        return budgetService.getAllBudgets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(@PathVariable("id") Long id) {
        return budgetService.getBudgetById(id);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(@PathVariable("id") Long id, @Valid @RequestBody CreateBudgetRequestDto createBudgetRequestDto) {
        return budgetService.updateBudget(id, createBudgetRequestDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable("id") Long id) {
        return budgetService.deleteBudget(id);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@Valid @RequestBody CreateBudgetRequestDto createBudgetRequestDto) {
        return budgetService.createBudget(createBudgetRequestDto);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<BudgetSummaryGraph>> getBudgetSummary() {
        return budgetService.getBudgetSummary();
    }

    @GetMapping("/themes")
    public ResponseEntity<ApiResponse<List<BudgetThemeResponse>>> getBudgetThemes() {
        return budgetService.getBudgetThemes();
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<BudgetCategoryResponse>>> getBudgetCategories() {
        return budgetService.getBudgetCategories();
    }

    @PostMapping("/add-transaction")
    public ResponseEntity<ApiResponse<BudgetResponse>> addTransactionToBudget(@Valid @RequestBody TransactionRequest transactionRequest) {
        return budgetService.addTransactionToBudget(transactionRequest);
    }
}
