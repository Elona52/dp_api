package com.api.item.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Item {

    // item_basic
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
    private Integer bidCount;                 // 입찰 횟수

    // item_detail
    private Long pbctNo;
    private Long orgBaseNo;
    private String cltrMnmtNo;
    private String nmrAddress;
    private String roadName;
    private String bldNo;
    private String bidStatus;
    private Integer viewCount;
    private String goodsDetail;
    private String assetCategory;
    private String bidRoundNo;
    private String feeRate;
    private Boolean jointBid;
    private Boolean electronicGuarantee;
    private Boolean agentBid;
}

