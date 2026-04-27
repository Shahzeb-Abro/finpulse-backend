package com.finpulse.mappers;

import com.finpulse.dto.response.ChatMessageResponse;
import com.finpulse.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    public ChatMessageResponse mapMessageDomainToResponseDto(ChatMessage domain) {
        if (domain == null) return null;

        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(domain.getId());
        dto.setContent(domain.getContent());
        dto.setRole(domain.getRole());
        dto.setCreatedAt(domain.getCreatedDate());
        dto.setLastUpdatedAt(domain.getLastUpdatedDate());

        return dto;
    }
}
