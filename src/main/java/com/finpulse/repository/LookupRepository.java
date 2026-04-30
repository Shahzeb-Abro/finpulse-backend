package com.finpulse.repository;

import com.finpulse.entity.Lookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Long> {
    List<Lookup> findByLookupType(String lookupType);
    Optional<Lookup> findByLookupTypeAndLookupValue(String lookupType, String lookupValue);
    List<Lookup> findByLookupValue(String lookupValue);

}
