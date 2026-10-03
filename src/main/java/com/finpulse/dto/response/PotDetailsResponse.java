package com.finpulse.dto.response;

import com.finpulse.entity.PotTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PotDetailsResponse {
    private PotResponse potResponse;
    private List<PotTransaction> potTransactions;
}
