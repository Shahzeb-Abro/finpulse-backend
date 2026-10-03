package com.finpulse.repository;

import com.finpulse.entity.Pot;
import com.finpulse.entity.PotTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PotTransactionRepository extends JpaRepository<PotTransaction, Long> {
    List<PotTransaction> findByPot(Pot pot);

    List<PotTransaction> findByPotOrderByCreatedDateDesc(Pot pot);
}
