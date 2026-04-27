package com.finpulse.controller;

import com.finpulse.dto.request.AssistantRequest;
import com.finpulse.dto.response.AssistantResponse;
import com.finpulse.service.AssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {
    private final AssistantService assistantService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantResponse> chat(@RequestBody AssistantRequest assistantRequest) {
        AssistantResponse assistantResponse = assistantRequest.getHistory() != null ? assistantService.chat(assistantRequest.getMessage(), assistantRequest.getHistory()) : assistantService.chat(assistantRequest.getMessage());
        return ResponseEntity.ok(assistantResponse);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamChat(@RequestBody AssistantRequest request) {
        StreamingResponseBody stream = outputStream -> {
            try {
                assistantService.streamChat(request.getMessage(), request.getHistory(), request.getSessionId(), outputStream);
            } catch (Exception e) {
                outputStream.write(("data: error\n\n").getBytes());
            }
        };

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(stream);
    }
}
