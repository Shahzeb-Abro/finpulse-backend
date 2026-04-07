package com.finpulse.repository;

import com.finpulse.entity.Lookup;
import com.finpulse.entity.Pot;
import com.finpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PotRepository extends JpaRepository<Pot, Long>, JpaSpecificationExecutor<Pot> {
    List<Pot> findByUser(User user);
    boolean existsByPotTheme(Lookup potThemeLkp);
    Pot findByIdAndUser(Long potId, User user);

    List<Pot> potTheme(Lookup potTheme);

    @Query("SELECT p.name FROM Pot p WHERE p.potTheme = :potThemeLkp")
    String findByPotTheme(@Param("potThemeLkp") Lookup potThemeLkp);
}
