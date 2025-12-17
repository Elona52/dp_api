package com.api.admin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemListResponse {
    private boolean success;
    private String source;
    private int page;
    private int size;
    private String sido;
    private int totalCount;
    private int currentPageCount;
    private List<?> items;  // Item 또는 ItemDetail 모두 받을 수 있도록 Object 사용
    private String message;
    private String errorType;
}

