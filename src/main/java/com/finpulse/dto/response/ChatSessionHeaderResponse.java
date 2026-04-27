package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSessionHeaderResponse {
    private Long id;
    private String title;
    private Date createdAt;
    private Date lastUpdatedAt;
}
