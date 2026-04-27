package com.finpulse.mappers;

import com.finpulse.dto.response.ChatSessionHeaderResponse;
import com.finpulse.entity.ChatSession;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionMapper {
    public ChatSessionHeaderResponse mapSessionToHeaderResponseDto(ChatSession domain) {
        if (domain == null) return null;

        ChatSessionHeaderResponse dto = new ChatSessionHeaderResponse();
        dto.setId(domain.getId());
        dto.setTitle(domain.getTitle());
        dto.setCreatedAt(domain.getCreatedDate());
        dto.setLastUpdatedAt(domain.getLastUpdatedDate());

        return dto;
    }
}
