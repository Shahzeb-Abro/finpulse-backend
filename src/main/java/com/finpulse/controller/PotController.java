package com.finpulse.controller;

import com.finpulse.constants.Constants;
import com.finpulse.dto.request.AddWithdrawMoneyPotRequest;
import com.finpulse.dto.request.PotRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.PotResponse;
import com.finpulse.service.PotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/v1/pots")
@RequiredArgsConstructor
public class PotController {
    private final PotService potService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PagedResponse<PotResponse>>> getAllPots(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String wildSearch,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int pageSize,
            @RequestParam(defaultValue = "createdDate,desc") String sort
    ) {
        return potService.getAllPots(new SearchDto(search, wildSearch, sort, page,pageSize));
    }

    @GetMapping("/{potId}")
    public ResponseEntity<ApiResponse<PotResponse>> getPotById(@PathVariable("potId") Long potId) {
        return potService.getPotById(potId);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PotResponse>> createPot(@Valid @RequestBody PotRequest potRequest) {
        return potService.createPot(potRequest);
    }

    @PutMapping("/edit/{potId}")
    public ResponseEntity<ApiResponse<PotResponse>> editPot(@PathVariable("potId") Long potId, @RequestBody PotRequest editPotDto) {
        return potService.editPot(potId, editPotDto);
    }

    @DeleteMapping("/{potId}")
    public ResponseEntity<ApiResponse<Void>> deletePotById(@PathVariable("potId") Long potId) {
        return potService.deletePotById(potId);
    }

    @PutMapping("/{potId}/add-money")
    public ResponseEntity<ApiResponse<PotResponse>> addMoneyToPot(@PathVariable("potId") Long potId, @RequestBody AddWithdrawMoneyPotRequest dto) {
        return potService.addMoneyToPot(potId, dto);
    }

    @PutMapping("{potId}/withdraw-money")
    public ResponseEntity<ApiResponse<PotResponse>> withdrawMoneyFromPot(@PathVariable("potId") Long potId, @RequestBody AddWithdrawMoneyPotRequest dto) {
        return potService.withdrawMoneyFromPot(potId, dto);
    }

    @GetMapping("/generate-key")
    public void generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey key = keyGen.generateKey();
        System.out.println(Base64.getEncoder()
                .encodeToString(key.getEncoded()));
    }

}
