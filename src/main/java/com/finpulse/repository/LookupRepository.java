package com.finpulse.repository;

import com.finpulse.entity.Lookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {
    List<Lookup> findByLookupType(String lookupType);
}
