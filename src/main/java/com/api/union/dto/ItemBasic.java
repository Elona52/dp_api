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
public class ItemBasic{

	private Integer rnum; // 목록 순번
	private Long plnmNo; // 물건번호 (PK)
	private String address; // 소재지 및 내역
	
    // 감정가
    private Long appraisalAmountMin; // 감정가 하한
    private Long appraisalAmountMax; // 감정가 상한

    // 최저입찰가
    private Long minBidPriceMin; // 최저입찰가 하한
    private Long minBidPriceMax; // 최저입찰가 상한
    
	private String orgName; // 담당계 / 집행기관
	private LocalDateTime bidStart; // 매각기일 시작
	private LocalDateTime bidEnd; // 매각기일 종료
	private String disposalMethod; // 처분방식
	private String bidMethod; // 입찰방식
	}
