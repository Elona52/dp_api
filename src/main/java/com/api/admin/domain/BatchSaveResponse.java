package com.api.admin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSaveResponse {
    private boolean success;      // 저장 성공 여부
    private String message;       // 메시지 ("성공", "일부 실패", "DB 오류" 등)
    private int savedCount;       // 실제 저장된 개수
    private int totalRequested;   // 요청한 전체 개수
    private String errorType;     // 오류 종류 (DB_ERROR, PARSE_ERROR 등)
}


