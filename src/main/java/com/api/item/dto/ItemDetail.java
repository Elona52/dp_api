package com.api.item.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.api.item.domain.Item;

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
    private Integer bidCount;                 // 입찰 횟수

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

    /**
     * ItemDetail을 Item domain으로 변환
     */
    public Item toItem() {
        Item item = new Item();
        item.setRnum(this.rnum);
        item.setPlnmNo(this.plnmNo);
        item.setAddress(this.address);
        item.setAppraisalAmountMin(this.appraisalAmountMin);
        item.setAppraisalAmountMax(this.appraisalAmountMax);
        item.setMinBidPriceMin(this.minBidPriceMin);
        item.setMinBidPriceMax(this.minBidPriceMax);
        item.setOrgName(this.orgName);
        item.setBidStart(this.bidStart);
        item.setBidEnd(this.bidEnd);
        item.setDisposalMethod(this.disposalMethod);
        item.setBidMethod(this.bidMethod);
        item.setBidCount(this.bidCount);
        item.setPbctNo(this.pbctNo);
        item.setOrgBaseNo(this.orgBaseNo);
        item.setCltrMnmtNo(this.cltrMnmtNo);
        item.setNmrAddress(this.nmrAddress);
        item.setRoadName(this.roadName);
        item.setBldNo(this.bldNo);
        item.setBidStatus(this.bidStatus);
        item.setViewCount(this.viewCount);
        item.setGoodsDetail(this.goodsDetail);
        item.setAssetCategory(this.assetCategory);
        item.setBidRoundNo(this.bidRoundNo);
        item.setFeeRate(this.feeRate);
        item.setJointBid(this.jointBid);
        item.setElectronicGuarantee(this.electronicGuarantee);
        item.setAgentBid(this.agentBid);
        return item;
    }

    /**
     * ItemDetail 리스트를 Item 리스트로 변환
     */
    public static List<Item> toItems(List<ItemDetail> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream()
            .map(ItemDetail::toItem)
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }

    /**
     * Item domain을 ItemDetail로 변환
     */
    public static ItemDetail from(Item item) {
        if (item == null) {
            return null;
        }
        return ItemDetail.builder()
            .rnum(item.getRnum())
            .plnmNo(item.getPlnmNo())
            .address(item.getAddress())
            .appraisalAmountMin(item.getAppraisalAmountMin())
            .appraisalAmountMax(item.getAppraisalAmountMax())
            .minBidPriceMin(item.getMinBidPriceMin())
            .minBidPriceMax(item.getMinBidPriceMax())
            .orgName(item.getOrgName())
            .bidStart(item.getBidStart())
            .bidEnd(item.getBidEnd())
            .disposalMethod(item.getDisposalMethod())
            .bidMethod(item.getBidMethod())
            .bidCount(item.getBidCount())
            .pbctNo(item.getPbctNo())
            .orgBaseNo(item.getOrgBaseNo())
            .cltrMnmtNo(item.getCltrMnmtNo())
            .nmrAddress(item.getNmrAddress())
            .roadName(item.getRoadName())
            .bldNo(item.getBldNo())
            .bidStatus(item.getBidStatus())
            .viewCount(item.getViewCount())
            .goodsDetail(item.getGoodsDetail())
            .assetCategory(item.getAssetCategory())
            .bidRoundNo(item.getBidRoundNo())
            .feeRate(item.getFeeRate())
            .jointBid(item.getJointBid())
            .electronicGuarantee(item.getElectronicGuarantee())
            .agentBid(item.getAgentBid())
            .build();
    }

    /**
     * Item 리스트를 ItemDetail 리스트로 변환
     */
    public static List<ItemDetail> fromItems(List<Item> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
            .map(ItemDetail::from)
            .filter(detail -> detail != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 주소에서 시도 추출
     */
    public String getSido() {
        if (nmrAddress != null && !nmrAddress.isEmpty()) {
            String[] parts = nmrAddress.split("\\s+");
            if (parts.length > 0 && (parts[0].endsWith("시") || parts[0].endsWith("도") || parts[0].endsWith("특별시") || parts[0].endsWith("광역시"))) {
                return parts[0];
            }
        }
        if (roadName != null && !roadName.isEmpty()) {
            String[] parts = roadName.split("\\s+");
            if (parts.length > 0 && (parts[0].endsWith("시") || parts[0].endsWith("도") || parts[0].endsWith("특별시") || parts[0].endsWith("광역시"))) {
                return parts[0];
            }
        }
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split("\\s+");
            if (parts.length > 0 && (parts[0].endsWith("시") || parts[0].endsWith("도") || parts[0].endsWith("특별시") || parts[0].endsWith("광역시"))) {
                return parts[0];
            }
        }
        return null;
    }
}
