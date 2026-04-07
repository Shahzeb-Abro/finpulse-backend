package com.finpulse.repository;

import com.finpulse.entity.Budget;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    boolean existsByBudgetCategoryAndUser(Lookup budgetCategoryId, User userId);
    boolean existsByBudgetThemeAndUser(Lookup budgetThemeId, User userId);

    List<Budget> findAllByUser(User user);

    Budget findByIdAndUser(Long id, User user);

    Budget findByBudgetCategoryAndUser(Lookup budgetCategoryId, User user);
}
