package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "CHAT_SESSIONS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "active_flag = true")
public class ChatSession extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Boolean activeFlag;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
