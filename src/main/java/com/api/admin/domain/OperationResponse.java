package com.api.admin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResponse {
    private boolean success;
    private String message;
    private Integer affectedRows;  // 영향받은 행 수 (삭제, 업데이트 등)
    private String errorType;       // 에러 타입 (예외 클래스명)
}

