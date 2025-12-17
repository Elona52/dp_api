package com.api.payment.dto;

import com.api.payment.domain.PaymentBase;
import com.api.payment.domain.PaymentDetail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Payment 도메인 객체를 DTO로 변환하는 Converter
 */
@Slf4j
@Component
public class PaymentConverter {

    /**
     * PaymentDetail과 PaymentBase를 PaymentResponse로 변환
     * payment_base.id = payment_detail.payment_id 조인 관계 사용
     * 
     * @param paymentDetail 결제 상세 정보
     * @param paymentBase 결제 기본 정보 (회원ID + 물건번호 기준)
     * @return PaymentResponse DTO
     */
    public PaymentResponse toResponse(PaymentDetail paymentDetail, PaymentBase paymentBase) {
        if (paymentDetail == null || paymentBase == null) {
            return null;
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setId(paymentDetail.getId());
        response.setItemId(paymentBase.getItemId());
        response.setCltrNo(paymentDetail.getCltrNo());
        response.setMemberId(paymentBase.getMemberId());
        response.setImpUid(paymentDetail.getImpUid());
        response.setMerchantUid(paymentDetail.getMerchantUid());
        response.setItemName(paymentDetail.getItemName());
        response.setAmount(paymentDetail.getAmount());
        response.setPaidAmount(paymentDetail.getPaidAmount());
        response.setPaymentMethod(paymentDetail.getPaymentMethod());
        response.setPgProvider(paymentDetail.getPgProvider());
        // pgTid는 민감정보이므로 제외
        response.setCardName(paymentDetail.getCardName());
        response.setCardNumber(paymentDetail.getCardNumber());
        response.setBuyerName(paymentDetail.getBuyerName());
        response.setBuyerEmail(paymentDetail.getBuyerEmail());
        response.setBuyerTel(paymentDetail.getBuyerTel());
        response.setBuyerAddr(paymentDetail.getBuyerAddr());
        response.setBuyerPostcode(paymentDetail.getBuyerPostcode());
        response.setStatus(paymentDetail.getStatus());
        response.setPaidAt(paymentDetail.getPaidAt());
        response.setFailedAt(paymentDetail.getFailedAt());
        response.setCancelledAt(paymentDetail.getCancelledAt());
        response.setFailReason(paymentDetail.getFailReason());
        response.setCancelReason(paymentDetail.getCancelReason());
        response.setReceiptUrl(paymentDetail.getReceiptUrl());
        response.setCreatedDate(paymentDetail.getCreatedAt());
        response.setUpdatedDate(paymentDetail.getUpdatedAt());
        return response;
    }
}

