package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistantResponse {
    private String reply;
    private Long sessionId;
}
