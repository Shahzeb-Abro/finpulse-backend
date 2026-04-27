package com.finpulse.service;

import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.ChatMessageResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.entity.ChatMessage;
import com.finpulse.entity.ChatSession;
import com.finpulse.entity.User;
import com.finpulse.mappers.ChatMessageMapper;
import com.finpulse.repository.ChatMessageRepository;
import com.finpulse.repository.ChatSessionRepository;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SecurityUtils securityUtils;
    private final ChatMessageMapper chatMessageMapper;


    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageResponse>>> getMessages(
            Long chatSessionId,
            Long cursor,
            int size
    ) {
        User user = securityUtils.getCurrentUser();
        ChatSession session = chatSessionRepository.findByIdAndUser(chatSessionId, user);

        if (session == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Chat session not found"));
        }

        Pageable pageable = PageRequest.of(0, size);

        Page<ChatMessage> page = cursor == null
                ? chatMessageRepository.findByChatSessionIdOrderByCreatedDateDesc(chatSessionId, pageable)
                : chatMessageRepository.findByChatSessionIdAndIdLessThanOrderByCreatedDateDesc(chatSessionId, cursor, pageable);

        Page<ChatMessageResponse> responsePage = page.map(chatMessageMapper::mapMessageDomainToResponseDto);

        return ResponseEntity.ok(ApiResponse.success(
                "Messages fetched successfully",
                PagedResponse.from(responsePage)));
    }
}
