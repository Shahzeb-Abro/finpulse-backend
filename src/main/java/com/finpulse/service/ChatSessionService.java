package com.finpulse.service;

import com.finpulse.constants.Constants;
import com.finpulse.dto.request.ChatSessionRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.ChatSessionHeaderResponse;
import com.finpulse.dto.response.PagedResponse;
import com.finpulse.entity.ChatSession;
import com.finpulse.entity.User;
import com.finpulse.mappers.ChatSessionMapper;
import com.finpulse.repository.ChatSessionRepository;
import com.finpulse.specification.GenericSpecificationBuilder;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionService {
    private final ChatSessionRepository chatSessionRepository;
    private final SecurityUtils securityUtils;
    private final ChatSessionMapper chatSessionMapper;

    public ResponseEntity<ApiResponse<PagedResponse<ChatSessionHeaderResponse>>> getAllChatSessionHeaders(SearchDto searchDto) {
        Pageable pageable = GenericSpecificationBuilder.buildPageable(searchDto.getPage(), searchDto.getPageSize(), searchDto.getSort());

        Specification<ChatSession> spec = GenericSpecificationBuilder.build(
                ChatSession.class,
                securityUtils.getCurrentUser(),
                searchDto.getSearch(),
                searchDto.getWildSearch()
        );

        Page<ChatSession> resultPage = chatSessionRepository.findAll(spec, pageable);
        Page<ChatSessionHeaderResponse> pageResponse = resultPage.map(chatSessionMapper::mapSessionToHeaderResponseDto);
        return ResponseEntity.ok(ApiResponse.success("Chat sessions fetched successfully", PagedResponse.from(pageResponse)));
    }

    public ResponseEntity<ApiResponse<ChatSessionHeaderResponse>> renameSession(ChatSessionRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();
        ChatSession chatSession = chatSessionRepository.findByIdAndUser(dto.getId(), loggedInUser);

        if (chatSession == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Session not found"));
        }

        if (dto.getTitle() == null || dto.getTitle().length() == 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Title is empty"));
        }
        if (dto.getTitle().length() > Constants.MAX_SESSION_TITLE_LENGTH) {
            return  ResponseEntity.badRequest().body(ApiResponse.error(String.format("Title is too long, max %s characters allowed", Constants.MAX_SESSION_TITLE_LENGTH)));
        }

        chatSession.setTitle(dto.getTitle());
        chatSessionRepository.save(chatSession);
        return ResponseEntity.ok().body(ApiResponse.success("Session renamed successfully", chatSessionMapper.mapSessionToHeaderResponseDto(chatSession)));
    }

    public ResponseEntity<ApiResponse> deleteChatSession(ChatSessionRequest dto) {
        User loggedInUser = securityUtils.getCurrentUser();
        ChatSession chatSession = chatSessionRepository.findByIdAndUser(dto.getId(), loggedInUser);
        if (chatSession == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Session not found"));
        }

        chatSession.setActiveFlag(Boolean.FALSE);

        chatSessionRepository.save(chatSession);
        return ResponseEntity.ok().body(ApiResponse.success("Session deleted successfully"));
    }
}
