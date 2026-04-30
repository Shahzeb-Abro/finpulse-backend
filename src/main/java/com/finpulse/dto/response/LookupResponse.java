package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LookupResponse {
    private Long id;
    private String visibleValue;
    private String lookupValue;
    private String lookupType;
    private Boolean isUserEditable;
    private String hiddenValue;
}
