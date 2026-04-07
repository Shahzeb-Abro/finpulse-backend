package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "LOOKUPS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "active_flag = true")
public class Lookup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lookupType;
    private String lookupValue;
    private String visibleValue;
    private String hiddenValue;
    private Boolean activeFlag;
    private Boolean isUserEditable;
}
