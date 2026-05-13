package com.fashion.promotionservice.dto.common;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class PaginationResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
}
