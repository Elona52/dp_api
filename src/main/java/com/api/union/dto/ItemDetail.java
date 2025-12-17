package com.api.union.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetail{

    // 기본정보 (item_basic)
    private Integer rnum;
    private Long plnmNo;
    private String address;
    private Long appraisalAmountMin; 
    private Long appraisalAmountMax; 
    private Long minBidPriceMin; 
    private Long minBidPriceMax; 
    private String orgName;
    private LocalDateTime bidStart;
    private LocalDateTime bidEnd;
    private String disposalMethod;
    private String bidMethod;

    // 상세정보 (item_detail)
    private Long pbctNo;                   // 입찰번호
    private Long orgBaseNo;                // 집행기관번호
    private String cltrMnmtNo;             // 관리번호
    private String nmrAddress;             // 지번주소
    private String roadName;               // 도로명
    private String bldNo;                  // 건물번호
    private String bidStatus;              // 입찰상태
    private Integer viewCount;             // 조회수
    private String goodsDetail;            // 면적/물건 상세
    private String assetCategory;          // 처분/자산구분
    private String bidRoundNo;             // 입찰회차
    private String feeRate;                // 수수료율
    private Boolean jointBid;              // 공동입찰 여부
    private Boolean electronicGuarantee;   // 전자보증서
    private Boolean agentBid;              // 대리입찰
}
