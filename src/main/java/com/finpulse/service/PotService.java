package com.finpulse.service;

import com.finpulse.dto.request.AddWithdrawMoneyPotRequest;
import com.finpulse.dto.request.PotRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.dto.response.PotResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.entity.Pot;
import com.finpulse.entity.User;
import com.finpulse.mappers.PotMapper;
import com.finpulse.repository.LookupRepository;
import com.finpulse.repository.PotRepository;
import com.finpulse.specification.GenericSpecificationBuilder;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PotService {
    private final PotRepository potRepository;
    private final LookupRepository lookupRepository;
    private final SecurityUtils securityUtils;
    private final PotMapper potMapper;

    public ResponseEntity<ApiResponse<PagedResponse<PotResponse>>> getAllPots(SearchDto searchDto) {
       Pageable pageable = GenericSpecificationBuilder.buildPageable(searchDto.getPage(), searchDto.getPageSize(), searchDto.getSort());

        Specification<Pot> spec = GenericSpecificationBuilder.build(
                Pot.class,
                securityUtils.getCurrentUser(),
                searchDto.getSearch(),
                searchDto.getWildSearch()
        );

        Page<Pot> resultPage = potRepository.findAll(spec, pageable);

        Page<PotResponse> pageResponse = resultPage.map(potMapper::mapPotDomainToResponseDto);

        return ResponseEntity.ok(ApiResponse.success("Pots fetched successfully", PagedResponse.from(pageResponse)));
    }

    public ResponseEntity<ApiResponse<PotResponse>> createPot(PotRequest dto) {
        if (dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Target amount must be greater than zero"));
        }

        Lookup potThemeLookup = lookupRepository.findById(dto.getThemeId()).orElseThrow();

        boolean existsByTheme = potRepository.existsByPotTheme(potThemeLookup);

        if (existsByTheme) {
            return ResponseEntity.badRequest().body(ApiResponse.error(String.format("Pot with theme '%s' already exists", potThemeLookup.getVisibleValue())));
        }

        User loggedInUser = securityUtils.getCurrentUser();
        Pot newPot = Pot.builder()
                .user(loggedInUser)
                .name(dto.getName())
                .targetAmount(dto.getTargetAmount())
                .totalSaved(BigDecimal.ZERO)
                .potTheme(potThemeLookup)
                .activeFlag(Boolean.TRUE)
                .build();

        potRepository.save(newPot);

        URI location = URI.create("/api/pots/" + newPot.getId());

        return ResponseEntity.created(location).body(ApiResponse.success("Pot created successfully",
                potMapper.mapPotDomainToResponseDto(newPot)
        ));
    }

    public ResponseEntity<ApiResponse<PotResponse>> editPot(Long potId, PotRequest editDto) {
        User loggedInUser = securityUtils.getCurrentUser();
        Pot existingPot = potRepository.findByIdAndUser(potId, loggedInUser);

        if (existingPot == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No such pot found"));
        }

        Lookup potThemeLookup = lookupRepository.findById(editDto.getThemeId()).orElseThrow();

        String potNameWithEditThemeId = potRepository.findByPotTheme(potThemeLookup);

        if (existingPot.getPotTheme().getId() != editDto.getThemeId() && potNameWithEditThemeId != null && !potNameWithEditThemeId.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(String.format("There is already a pot named '%s' with theme '%s'. Please choose some other theme.", potNameWithEditThemeId, potThemeLookup.getVisibleValue())));
        }

        existingPot.setName(editDto.getName());
        existingPot.setTargetAmount(editDto.getTargetAmount());
        existingPot.setPotTheme(potThemeLookup);
        potRepository.save(existingPot);

        return ResponseEntity.ok().body(ApiResponse.success("Pot updated successfully",
                potMapper.mapPotDomainToResponseDto(existingPot)
                ));
    }

    public ResponseEntity<ApiResponse<PotResponse>> getPotById(Long potId) {
        User loggedInUser = securityUtils.getCurrentUser();

        Pot pot = potRepository.findByIdAndUser(potId, loggedInUser);

        if (pot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("No pot with given identifier"));
        }

        return ResponseEntity.ok().body(ApiResponse.success("Pot found",
              potMapper.mapPotDomainToResponseDto(pot)
                ));
    }

    public ResponseEntity<ApiResponse<Void>> deletePotById(Long potId) {
        User loggedInUser = securityUtils.getCurrentUser();

        Pot pot = potRepository.findByIdAndUser(potId, loggedInUser);

        if (potId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No pot found with given identifier"));
        }

        pot.setActiveFlag(Boolean.FALSE);
        potRepository.save(pot);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success("Pot deleted successfully"));
    }

    public ResponseEntity<ApiResponse<PotResponse>> addMoneyToPot(Long potId, AddWithdrawMoneyPotRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();
        Pot pot = potRepository.findByIdAndUser(potId, loggedInUser);

        if (pot == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No pot with given identifier was found"));
        }

        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount must be greater than zero"));
        }

        BigDecimal remainingToTarget = pot.getTargetAmount().subtract(pot.getTotalSaved());

        if (dto.getAmount().compareTo(remainingToTarget) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Amount exceeds the remaining target amount"));
        }

        BigDecimal prevSavedAmount = pot.getTotalSaved();
        pot.setTotalSaved(prevSavedAmount.add(dto.getAmount()));
        potRepository.save(pot);

        return ResponseEntity.ok().body(ApiResponse.success("Amount added successfully",
               potMapper.mapPotDomainToResponseDto(pot)
                ));
    }

    public ResponseEntity<ApiResponse<PotResponse>> withdrawMoneyFromPot(Long potId, AddWithdrawMoneyPotRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();
        Pot pot = potRepository.findByIdAndUser(potId, loggedInUser);

        if (pot == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No pot with given identifier was found"));
        }

        BigDecimal savedAmount = pot.getTotalSaved();
        if (dto.getAmount().compareTo(savedAmount) > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Withdrawal amount exceeds the total saved amount"));
        }

        pot.setTotalSaved(savedAmount.subtract(dto.getAmount()));
        potRepository.save(pot);

        return ResponseEntity.ok().body(ApiResponse.success("Amount withdrawn successfully",
              potMapper.mapPotDomainToResponseDto(pot)
                ));
    }
}


