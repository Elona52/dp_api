package com.api.payment.dto;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import com.api.item.dto.ItemDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data  
@Builder  
@NoArgsConstructor  
@AllArgsConstructor  
public class PaymentResponse {
    
    private Long id;
    private Integer auctionNo;
    private Long itemId;
    private String cltrNo;
    private String memberId;
    
    // 아임포트 정보
    private String impUid;
    private String merchantUid;
    private String itemName;            // 상품명
    private Long amount;
    private Long paidAmount;
    private String paymentMethod;
    private String pgProvider;
    // pgTid는 민감정보이므로 제외
    
    // 카드 정보 (마스킹된 정보만)
    private String cardName;
    private String cardNumber;
    
    // 구매자 정보
    private String buyerName;
    private String buyerEmail;
    private String buyerTel;
    private String buyerAddr;
    private String buyerPostcode;
    
    // 결제 상태 및 시간
    private String status;
    private Timestamp paidAt;
    private Timestamp failedAt;
    private Timestamp cancelledAt;
    private Timestamp deadlineDate;  // 입찰 마감일시
    
    // 실패/취소 사유
    private String failReason;
    private String cancelReason;
    
    // 기타
    private String receiptUrl;
    private Timestamp createdDate;
    private Timestamp updatedDate;
    
    // 포맷된 날짜 문자열 (템플릿에서 사용)
    private String formattedCreatedDate;
    private String formattedDeadlineDate;
    
    /**
     * PaymentDetail과 PaymentBase를 PaymentResponse로 변환
     * payment_base.id = payment_detail.payment_id 조인 관계 사용
     * 
     * @deprecated PaymentConverter.toResponse()를 사용하세요
     * @param paymentDetail 결제 상세 정보
     * @param paymentBase 결제 기본 정보 (회원ID + 물건번호 기준)
     * @return PaymentResponse DTO
     */
    @Deprecated
    public static PaymentResponse from(com.api.payment.domain.PaymentDetail paymentDetail, 
                                       com.api.payment.domain.PaymentBase paymentBase) {
        // PaymentConverter를 사용하는 것을 권장합니다
        // 이 메서드는 호환성을 위해 유지됩니다
        if (paymentDetail == null || paymentBase == null) {
            return null;
        }
        
        return PaymentResponse.builder()
                .id(paymentDetail.getId())
                .itemId(paymentBase.getItemId())
                .cltrNo(paymentDetail.getCltrNo())
                .memberId(paymentBase.getMemberId())
                .impUid(paymentDetail.getImpUid())
                .merchantUid(paymentDetail.getMerchantUid())
                .itemName(paymentDetail.getItemName())
                .amount(paymentDetail.getAmount())
                .paidAmount(paymentDetail.getPaidAmount())
                .paymentMethod(paymentDetail.getPaymentMethod())
                .pgProvider(paymentDetail.getPgProvider())
                .cardName(paymentDetail.getCardName())
                .cardNumber(paymentDetail.getCardNumber())
                .buyerName(paymentDetail.getBuyerName())
                .buyerEmail(paymentDetail.getBuyerEmail())
                .buyerTel(paymentDetail.getBuyerTel())
                .buyerAddr(paymentDetail.getBuyerAddr())
                .buyerPostcode(paymentDetail.getBuyerPostcode())
                .status(paymentDetail.getStatus())
                .paidAt(paymentDetail.getPaidAt())
                .failedAt(paymentDetail.getFailedAt())
                .cancelledAt(paymentDetail.getCancelledAt())
                .failReason(paymentDetail.getFailReason())
                .cancelReason(paymentDetail.getCancelReason())
                .receiptUrl(paymentDetail.getReceiptUrl())
                .createdDate(paymentDetail.getCreatedAt())
                .updatedDate(paymentDetail.getUpdatedAt())
                .build();
    }
    
    /**
     * 날짜 포맷팅 헬퍼 메서드
     */
    private static String formatTimestamp(Timestamp timestamp, String pattern) {
        if (timestamp == null) {
            return "-";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(timestamp);
        } catch (Exception e) {
            return "-";
        }
    }
    
    /**
     * 포맷된 날짜 문자열 설정
     */
    public void formatDates() {
        this.formattedCreatedDate = formatTimestamp(this.createdDate, "yyyy-MM-dd HH:mm");
        this.formattedDeadlineDate = formatTimestamp(this.deadlineDate, "yyyy-MM-dd HH:mm");
    }
    
    /**
     * ItemDetail 정보로 PaymentResponse 보강
     * deadlineDate, itemName, cltrNo, itemId 등을 ItemDetail에서 설정
     * 
     * @param itemDetail ItemDetail 객체
     */
    public void enrichWithItemDetail(ItemDetail itemDetail) {
        if (itemDetail == null) {
            return;
        }
        
        try {
            // deadlineDate 설정
            if (itemDetail.getBidEnd() != null) {
                java.time.LocalDateTime bidEnd = itemDetail.getBidEnd();
                this.deadlineDate = Timestamp.valueOf(bidEnd);
            }
            
            // itemName 설정 (없거나 비어있을 때만)
            if (this.itemName == null || this.itemName.isEmpty()) {
                this.itemName = itemDetail.getAddress() != null 
                    ? itemDetail.getAddress() 
                    : itemDetail.getGoodsDetail();
            }
            
            // cltrNo 설정 (없거나 비어있을 때만)
            if ((this.cltrNo == null || this.cltrNo.isEmpty()) && itemDetail.getCltrMnmtNo() != null) {
                this.cltrNo = itemDetail.getCltrMnmtNo();
            }
            
            // itemId 설정 (없을 때만)
            if (this.itemId == null && itemDetail.getPlnmNo() != null) {
                this.itemId = itemDetail.getPlnmNo();
            }
        } catch (Exception e) {
            log.warn("ItemDetail 정보로 PaymentResponse 보강 실패: {}", e.getMessage());
        }
    }
    
    
    /**
     * ItemDetail에서 deadlineDate 설정
     * 
     * @param itemDetail ItemDetail 객체
     */
    public void setDeadlineDateFromItemDetail(ItemDetail itemDetail) {
        if (itemDetail != null && itemDetail.getBidEnd() != null) {
            try {
                java.time.LocalDateTime bidEnd = itemDetail.getBidEnd();
                this.deadlineDate = Timestamp.valueOf(bidEnd);
            } catch (Exception e) {
                log.warn("deadlineDate 설정 실패: {}", e.getMessage());
            }
        }
    }
}

