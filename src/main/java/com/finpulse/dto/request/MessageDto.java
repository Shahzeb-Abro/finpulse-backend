package com.finpulse.dto.request;

import lombok.Data;

@Data
public class MessageDto {
    private String role; // "user" or "assistant"
    private String content;
}
