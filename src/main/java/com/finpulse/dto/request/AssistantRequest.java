package com.finpulse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AssistantRequest {
    private String message;
    private List<MessageDto> history;
    private Long sessionId;
}
