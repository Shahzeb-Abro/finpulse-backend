package com.finpulse.controller;

import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.LookupResponse;
import com.finpulse.service.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/lookups")
@RequiredArgsConstructor
public class LookupController {
    private final LookupService lookupService;

    @GetMapping("/find-all-by-type-and-value")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllLookupsByTypeAndValue(
            @RequestParam("lookupType") String lookupType,
            @RequestParam("lookupValue") String lookupValue
    ) {
        return lookupService.getAllByLookupTypeAndLookupValue(lookupType, lookupValue);
    }


    @GetMapping("/find-all-by-type")
    public ResponseEntity<ApiResponse<List<LookupResponse>>> getAllLookupsByType(
            @RequestParam("lookupType") String lookupType
    ) {
        return lookupService.getAllByLookupType(lookupType);
    }
}
