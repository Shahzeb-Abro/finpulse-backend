package com.finpulse.entity;

import com.finpulse.security.FieldEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Entity
@Table(name = "POTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "active_flag = true")
public class Pot extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Convert(converter = FieldEncryptor.class)
    private String name;
    private BigDecimal targetAmount = BigDecimal.ZERO;
    private BigDecimal totalSaved = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "pot_theme_id")
    private Lookup potTheme;

    private Boolean activeFlag;

}
