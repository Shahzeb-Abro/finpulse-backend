package com.finpulse.mappers;

import com.finpulse.dto.response.BudgetResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.dto.response.budgetSummaryGraph.BudgetSummaryItem;
import com.finpulse.entity.Budget;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.enums.LookupTypeEnum;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BudgetMapper {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final LookupRepository lookupRepository;

    public BudgetResponse mapBudgetDomainToResponseDto(Budget domain) {
        if (domain == null) return null;

        BudgetResponse dto = new BudgetResponse();
        dto.setId(domain.getId());

        dto.setBudgetCategoryId(domain.getBudgetCategory().getId());
        dto.setBudgetThemeId(domain.getBudgetTheme().getId());
        dto.setBudgetThemeVisibleValue(domain.getBudgetTheme().getVisibleValue());
        dto.setBudgetThemeLookupValue(domain.getBudgetTheme().getLookupValue());
        dto.setBudgetCategoryVisibleValue(domain.getBudgetCategory().getVisibleValue());
        dto.setBudgetCategoryLookupValue(domain.getBudgetCategory().getLookupValue());
        dto.setBudgetPeriodId(domain.getBudgetPeriod().getId());
        dto.setBudgetPeriodLookupValue(domain.getBudgetPeriod().getLookupValue());
        dto.setBudgetPeriodVisibleValue(domain.getBudgetPeriod().getVisibleValue());
        dto.setStartDate(domain.getStartDate());
        dto.setEndDate(domain.getEndDate());
        dto.setMaximumSpend(domain.getMaximumSpend());
        dto.setRemainingSpend(domain.getRemainingSpend());


        // Find transactions of the budget
        Lookup budgetCategory = domain.getBudgetCategory();
        Lookup transactionCategory = lookupRepository.findByLookupTypeAndLookupValue(LookupTypeEnum.TRANSACTION_CATEGORY.name(), budgetCategory.getLookupValue()).orElse(null);
        BigDecimal currentSpend = transactionRepository.findTotalAmountByCategoryAndUser(transactionCategory, domain.getUser());
        if (currentSpend != null) {
            dto.setCurrentSpend(currentSpend);
        } else {
            dto.setCurrentSpend(BigDecimal.ZERO);
        }
        if (transactionCategory == null) return dto;
        List<Transaction> budgetTransactions = transactionRepository.findTop3ByCategoryAndUserOrderByCreatedDateDesc(transactionCategory, domain.getUser());


        if (!budgetTransactions.isEmpty()) {
            List<TransactionResponse> transactionResponses = budgetTransactions.stream()
                    .map(transactionMapper::mapDomainToResponseDto)
                    .toList();

            dto.setTransactions(transactionResponses);
        }
        return dto;
    }

    public BudgetSummaryItem mapDomainToBudgetSummaryItemDto(Budget domain) {
        if (domain == null) return null;

        BudgetSummaryItem dto = new BudgetSummaryItem();
        dto.setId(domain.getId());
        dto.setName(domain.getBudgetCategory().getVisibleValue());
        dto.setMaximumSpend(domain.getMaximumSpend());
        dto.setCurrentSpend(domain.getCurrentSpend());
        dto.setTheme(domain.getBudgetTheme().getLookupValue());

        return dto;
    }

    public BudgetSummaryItem mapDomainToBudgetSummaryItemDto(Budget domain, BigDecimal currentSpend) {
        if (domain == null) return null;

        BudgetSummaryItem dto = new BudgetSummaryItem();
        dto.setId(domain.getId());
        dto.setName(domain.getBudgetCategory().getVisibleValue());
        dto.setMaximumSpend(domain.getMaximumSpend());
        dto.setCurrentSpend(currentSpend);
        dto.setTheme(domain.getBudgetTheme().getLookupValue());

        return dto;
    }

    public BudgetResponse mapBudgetTransactionDomainToResponseDto(Budget budget, List<Transaction> transactions) {
        if (budget == null) return null;

        BudgetResponse dto = new BudgetResponse();
        dto.setId(budget.getId());

        dto.setBudgetCategoryId(budget.getBudgetCategory().getId());
        dto.setBudgetThemeId(budget.getBudgetTheme().getId());
        dto.setBudgetThemeVisibleValue(budget.getBudgetTheme().getVisibleValue());
        dto.setBudgetThemeLookupValue(budget.getBudgetTheme().getLookupValue());
        dto.setBudgetCategoryVisibleValue(budget.getBudgetCategory().getVisibleValue());
        dto.setBudgetCategoryLookupValue(budget.getBudgetCategory().getLookupValue());
        dto.setMaximumSpend(budget.getMaximumSpend());
        dto.setCurrentSpend(budget.getCurrentSpend());
        dto.setRemainingSpend(budget.getRemainingSpend());


        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(transactionMapper::mapDomainToResponseDto)
                .toList();

        dto.setTransactions(transactionResponses);

        return dto;
    }
}
