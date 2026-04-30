package com.finpulse.service;

import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.LookupResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.mappers.LookupMapper;
import com.finpulse.repository.LookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LookupService {
    private final LookupRepository lookupRepository;
    private final LookupMapper lookupMapper;

    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllByLookupTypeAndLookupValue(String lookupType, String lookupValue) {
        Optional<Lookup> lookups = lookupRepository.findByLookupTypeAndLookupValue(lookupType, lookupValue);

        if (lookups.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No lookups found", List.of()));
        }

        List<LookupResponse> mappedLookups = lookups.stream().map(lookupMapper::mapLookupDomainToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lookups fetched successfully", mappedLookups));
    }

    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllByLookupType(String lookupType) {
        List<Lookup> lookups = lookupRepository.findByLookupType(lookupType);

        if (lookups.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No lookups found", List.of()));
        }

        List<LookupResponse> mappedLookups = lookups.stream().map(lookupMapper::mapLookupDomainToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Lookups fetched successfully", mappedLookups));
    }
}
