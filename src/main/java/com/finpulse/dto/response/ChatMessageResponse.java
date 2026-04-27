package com.finpulse.dto.response;

import com.finpulse.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String content;
    private ChatRole role;
    private Long chatSessionId;
    private Date createdAt;
    private Date lastUpdatedAt;
}
