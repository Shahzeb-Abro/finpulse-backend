package com.finpulse.entity;

import com.finpulse.enums.ChatRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "CHAT_MESSAGES")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Where(clause = "active_flag = true")
public class ChatMessage extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;
    private Boolean activeFlag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @ManyToOne
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;
}
