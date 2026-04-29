package com.finpulse.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Transaction;
import com.finpulse.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByCategoryAndUser(Lookup category, User user);

    List<Transaction> findAllByUser(User user);

    Transaction findByIdAndUser(Long transactionId, User user);

    List<Transaction> findTop3ByCategoryAndUserOrderByCreatedDateDesc(Lookup category, User user);
}
