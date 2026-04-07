package com.finpulse.mappers;

import com.finpulse.dto.response.BudgetResponse;
import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.dto.response.budgetSummaryGraph.BudgetSummaryItem;
import com.finpulse.entity.Budget;
import com.finpulse.entity.Transaction;
import com.finpulse.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BudgetMapper {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

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
        dto.setMaximumSpend(domain.getMaximumSpend());
        dto.setCurrentSpend(domain.getCurrentSpend());
        dto.setRemainingSpend(domain.getRemainingSpend());

        // Find transactions of the budget
        List<Transaction> budgetTransactions = transactionRepository.findTop3ByCategoryAndUserOrderByCreatedDateDesc(domain.getBudgetCategory(), domain.getUser());

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
