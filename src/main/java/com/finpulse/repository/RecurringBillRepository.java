package com.finpulse.repository;

import com.finpulse.entity.RecurringBill;
import com.finpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RecurringBillRepository extends JpaRepository<RecurringBill, Long>, JpaSpecificationExecutor<RecurringBill> {
    RecurringBill findByIdAndUser(Long id, User user);
}
