package com.api.payment.domain;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * payment_base 테이블 도메인
 * 입찰 기본 정보 (회원, 물건, 입찰가격)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBase {
    
    private Long id;
    private String memberId;        // 구매자 회원 ID
    private Long itemId;             // API 아이템 ID (item_basic.plnm_no)
    private Long bidPrice;           // 입찰 금액
    private Timestamp createdAt;     // 생성일
}

