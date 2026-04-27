package com.finpulse.repository;

import com.finpulse.entity.ChatSession;
import com.finpulse.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long>, JpaSpecificationExecutor<ChatSession> {
    ChatSession findByIdAndUser(Long chatSessionId, User user);
}
