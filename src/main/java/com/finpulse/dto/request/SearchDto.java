package com.finpulse.dto.request;

import com.finpulse.constants.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDto {
    private String search;
    private String wildSearch;
    private String sort;
    private int page = Integer.parseInt(Constants.DEFAULT_PAGE_NUMBER);
    private int pageSize = Integer.parseInt(Constants.DEFAULT_PAGE_SIZE);
}
