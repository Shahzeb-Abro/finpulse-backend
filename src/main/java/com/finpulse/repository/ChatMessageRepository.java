package com.finpulse.repository;

import com.finpulse.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    Page<ChatMessage> findByChatSessionIdOrderByCreatedDateDesc(Long chatSessionId, Pageable pageable);
    Page<ChatMessage> findByChatSessionIdAndIdLessThanOrderByCreatedDateDesc(
            Long chatSessionId, Long cursorId, Pageable pageable);

}
