package com.finpulse.service;

import com.finpulse.dto.request.CreateBudgetRequestDto;
import com.finpulse.dto.request.TransactionRequest;
import com.finpulse.dto.response.*;
import com.finpulse.dto.response.budgetSummaryGraph.BudgetSummaryGraph;
import com.finpulse.dto.response.budgetSummaryGraph.BudgetSummaryItem;
import com.finpulse.entity.Budget;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import com.finpulse.enums.TransactionType;
import com.finpulse.mappers.BudgetMapper;
import com.finpulse.mappers.TransactionMapper;
import com.finpulse.repository.BudgetRepository;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.TransactionRepository;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.finpulse.enums.LookupTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {
    private final LookupRepository lookupRepository;
    private final BudgetRepository budgetRepository;
    private final SecurityUtils securityUtils;
    private final BudgetMapper budgetMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public ResponseEntity<ApiResponse<List<BudgetResponse>>> getAllBudgets() {
        User loggedInUser = securityUtils.getCurrentUser();

        List<Budget> budgets = budgetRepository.findAllByUser(loggedInUser);

        List<BudgetResponse> mappedBudgets = budgets.stream()
                .map(budget -> budgetMapper.mapBudgetDomainToResponseDto(budget))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Budgets fetched successfully", mappedBudgets));
    }

    public ResponseEntity<ApiResponse<BudgetResponse>> getBudgetById(Long id) {
        User loggedInUser = securityUtils.getCurrentUser();

        Budget budget = budgetRepository.findByIdAndUser(id, loggedInUser);

        if (budget == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Budget not found"));
        }

        return ResponseEntity.ok(ApiResponse.success("Budget fetched successfully", budgetMapper.mapBudgetDomainToResponseDto(budget)));
    }

    public ResponseEntity<ApiResponse<BudgetResponse>> updateBudget(Long id, CreateBudgetRequestDto dto) {
        User loggedInUser = securityUtils.getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUser(id, loggedInUser);

        if (budget == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Budget not found"));
        }

        Lookup budgetTheme = lookupRepository.findById(dto.getBudgetThemeId()).orElseThrow();
        Lookup budgetCategory = lookupRepository.findById(dto.getBudgetCategoryId()).orElseThrow();
        Lookup budgetPeriod = lookupRepository.findById(dto.getBudgetPeriodId()).orElseThrow();
        boolean budgetAlreadyExistsByCategory = budgetRepository.existsByBudgetCategoryAndUser(budgetCategory, loggedInUser);
        boolean budgetAlreadyExistsByTheme = budgetRepository.existsByBudgetThemeAndUser(budgetTheme, loggedInUser);

        if (budgetAlreadyExistsByCategory && !budget.getBudgetCategory().getId().equals(budgetCategory.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Budget already exists for this category"));
        }

        if (budgetAlreadyExistsByTheme && !budget.getBudgetTheme().getId().equals(budgetTheme.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Budget already exists for this theme"));
        }

        budget.setBudgetTheme(budgetTheme);
        budget.setBudgetCategory(budgetCategory);
        budget.setMaximumSpend(dto.getMaximumSpend());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setBudgetPeriod(budgetPeriod);
        budgetRepository.save(budget);

        return ResponseEntity.ok(ApiResponse.success("Budget updated successfully", budgetMapper.mapBudgetDomainToResponseDto(budget)));
    }

    public ResponseEntity<ApiResponse<Void>> deleteBudget(Long id) {
        User loggedInUser = securityUtils.getCurrentUser();
        Budget budget = budgetRepository.findByIdAndUser(id, loggedInUser);

        if (budget == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Budget not found"));
        }

        budgetRepository.delete(budget);

        return ResponseEntity.ok(ApiResponse.success("Budget deleted successfully"));
    }

    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(CreateBudgetRequestDto dto) {
        User currentUser = securityUtils.getCurrentUser();
        Lookup budgetTheme = lookupRepository.findById(dto.getBudgetThemeId()).orElseThrow();
        Lookup budgetCategory = lookupRepository.findById(dto.getBudgetCategoryId()).orElseThrow();
        Lookup budgetPeriod = lookupRepository.findById(dto.getBudgetPeriodId()).orElseThrow();
        boolean budgetAlreadyExistsWithCategory = budgetRepository.existsByBudgetCategoryAndUser(budgetCategory, currentUser);
        boolean budgetAlreadyExistsWithTheme = budgetRepository.existsByBudgetThemeAndUser(budgetTheme, currentUser);

        if (budgetAlreadyExistsWithCategory) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Budget already exists for this category"));
        }

        if (budgetAlreadyExistsWithTheme) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Budget already exists for this theme"));
        }

        Budget newBudget = Budget.builder()
                .user(currentUser)
                .budgetCategory(budgetCategory)
                .budgetTheme(budgetTheme)
                .maximumSpend(dto.getMaximumSpend())
                .currentSpend(BigDecimal.ZERO)
                .remainingSpend(dto.getMaximumSpend())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .budgetPeriod(budgetPeriod)
                .build();

        budgetRepository.save(newBudget);

        return ResponseEntity.ok(ApiResponse.success("Budget created successfully", budgetMapper.mapBudgetDomainToResponseDto(newBudget)));
    }

    public ResponseEntity<ApiResponse<BudgetSummaryGraph>> getBudgetSummary() {
        User loggedInUser = securityUtils.getCurrentUser();
        List<Budget> budgets = budgetRepository.findAllByUser(loggedInUser);

        if (budgets.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No budgets found", new BudgetSummaryGraph()));
        }

        BudgetSummaryGraph budgetSummaryGraph = new BudgetSummaryGraph();
        List<BudgetSummaryItem> summaryItems = new ArrayList<>();

        budgets.forEach(budget -> {
            BigDecimal prevTotalMaximumSpend = budgetSummaryGraph.getTotalMaximumSpend();
            BigDecimal prevTotalCurrentSpend = budgetSummaryGraph.getTotalCurrentSpend();

            Lookup budgetCategory = budget.getBudgetCategory();
            Lookup transactionCategory = lookupRepository.findByLookupTypeAndLookupValue(LookupTypeEnum.TRANSACTION_CATEGORY.name(), budgetCategory.getLookupValue()).orElse(null);
            BigDecimal currentSpend = transactionRepository.findTotalAmountByCategoryAndUser(transactionCategory, budget.getUser());
            budgetSummaryGraph.setTotalMaximumSpend(prevTotalMaximumSpend.add(budget.getMaximumSpend()));
            budgetSummaryGraph.setTotalCurrentSpend(prevTotalCurrentSpend.add(currentSpend != null ? currentSpend : BigDecimal.ZERO));

            BudgetSummaryItem summaryItem = budgetMapper.mapDomainToBudgetSummaryItemDto(budget, currentSpend);
            summaryItems.add(summaryItem);
        });

        budgetSummaryGraph.setBudgetSummaryItems(summaryItems);

        return ResponseEntity.ok(ApiResponse.success("Budget summary fetched successfully", budgetSummaryGraph));

    }

    public ResponseEntity<ApiResponse<List<BudgetThemeResponse>>> getBudgetThemes() {
        List<Lookup> themes = lookupRepository.findByLookupType(LookupTypeEnum.THEME.name());

        List<BudgetThemeResponse> responses = new ArrayList<>();
        for (Lookup lookup : themes) {
            BudgetThemeResponse budgetTheme = BudgetThemeResponse.builder()
                    .id(lookup.getId())
                    .visibleValue(lookup.getVisibleValue())
                    .lookupType(lookup.getLookupType())
                    .lookupValue(lookup.getLookupValue())
                    .build();

            responses.add(budgetTheme);
        }

        return ResponseEntity.ok(ApiResponse.success("Budget themes fetched successfully", responses));
    }

    public ResponseEntity<ApiResponse<List<BudgetCategoryResponse>>> getBudgetCategories() {
        List<Lookup> categories = lookupRepository.findByLookupType(LookupTypeEnum.BUDGET_CATEGORY.name());

        List<BudgetCategoryResponse> responses = new ArrayList<>();
        for (Lookup lookup : categories) {
            BudgetCategoryResponse budgetCategories = BudgetCategoryResponse.builder()
                    .id(lookup.getId())
                    .visibleValue(lookup.getVisibleValue())
                    .lookupType(lookup.getLookupType())
                    .lookupValue(lookup.getLookupValue())
                    .doesAlreadyExist(Boolean.FALSE)
                    .build();

            responses.add(budgetCategories);
        }

        return ResponseEntity.ok(ApiResponse.success("Budget themes fetched successfully", responses));
    }

    public ResponseEntity<ApiResponse<BudgetResponse>> addTransactionToBudget(TransactionRequest dto) {
        Lookup category = lookupRepository.findById(dto.getCategoryId()).orElseThrow();
        User loggedInUser = securityUtils.getCurrentUser();

        if (category == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Budget not found"));
        }

        Budget budget = budgetRepository.findByBudgetCategoryAndUser(category, loggedInUser);

        if (budget == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Budget not found"));
        }

        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount must be greater than zero"));
        }

        BigDecimal remainingSpend = budget.getRemainingSpend();
        if (TransactionType.INCOME.name().equals(dto.getTransactionType().name()) && dto.getAmount().compareTo(remainingSpend) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount exceeds remaining spend"));
        }

        Transaction newTransaction = Transaction.builder()
                .user(loggedInUser)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .transactionType(dto.getTransactionType())
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now())
                .category(category)
                .activeFlag(Boolean.TRUE)
                .build();

        transactionRepository.save(newTransaction);

        budget.setCurrentSpend(budget.getCurrentSpend().add(dto.getAmount()));
        budget.setRemainingSpend(remainingSpend.subtract(dto.getAmount()));
        budgetRepository.save(budget);

        return ResponseEntity.status(201).body(ApiResponse.success("Transaction added successfully", budgetMapper.mapBudgetTransactionDomainToResponseDto(budget, List.of(newTransaction))));
    }
}
