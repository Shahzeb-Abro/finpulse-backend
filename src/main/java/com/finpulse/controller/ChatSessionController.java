package com.finpulse.controller;

import com.finpulse.constants.Constants;
import com.finpulse.dto.request.ChatSessionRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.service.ChatMessageService;
import com.finpulse.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat-sessions")
@RequiredArgsConstructor
public class ChatSessionController {
    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;

    @GetMapping("/")
    public ResponseEntity<?> getAllChatSessions( @RequestParam(required = false) String search,
                                                 @RequestParam(required = false) String wildSearch,
                                                 @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
                                                 @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int pageSize,
                                                 @RequestParam(defaultValue = "createdDate,desc") String sort) {
        SearchDto searchDto = new SearchDto(search, wildSearch, sort, page, pageSize);
        return chatSessionService.getAllChatSessionHeaders(searchDto);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<?> getChatSessionMessages(
            @PathVariable("sessionId") String sessionId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int pageSize
    ) {
        return chatMessageService.getMessages(Long.parseLong(sessionId), cursor, pageSize);
    }

    @PatchMapping("/{sessionId}/rename")
    public ResponseEntity<?> renameChatSession(@RequestBody ChatSessionRequest request, @PathVariable("sessionId") String sessionId) {
        ChatSessionRequest dto = new ChatSessionRequest();
        dto.setId(Long.parseLong(sessionId));
        dto.setTitle(request.getTitle());
        return chatSessionService.renameSession(dto);

    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteChatSession(@PathVariable("sessionId") String sessionId) {
        ChatSessionRequest dto = new ChatSessionRequest();
        dto.setId(Long.parseLong(sessionId));

        return chatSessionService.deleteChatSession(dto);
    }
}
