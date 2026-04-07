package com.finpulse.repository;

import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCategoryAndUser(Lookup category, User user);
    List<Transaction> findAllByUser(User user);

    Transaction findByIdAndUser(Long transactionId, User user);

    List<Transaction> findTop3ByCategoryAndUserOrderByCreatedDateDesc(Lookup category, User user);}
