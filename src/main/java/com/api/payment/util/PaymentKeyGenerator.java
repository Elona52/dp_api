package com.api.payment.util;

import com.api.payment.domain.PaymentBase;
import com.api.payment.domain.PaymentDetail;
import org.springframework.stereotype.Component;

/**
 * Payment의 고유 키 생성 유틸리티
 * 같은 물건을 나타내는 payment는 같은 키를 반환해야 함 (중복 체크용)
 */
@Component
public class PaymentKeyGenerator {

    /**
     * PaymentDetail의 고유 키 생성 (중복 체크용)
     */
    public String generateKey(PaymentDetail paymentDetail, PaymentBase paymentBase) {
        if (paymentDetail == null || paymentBase == null) {
            return "null";
        }
        
        // cltrNo가 있으면 우선 사용 (더 고유함)
        if (paymentDetail.getCltrNo() != null && !paymentDetail.getCltrNo().isEmpty()) {
            return "cltr_" + paymentDetail.getCltrNo();
        }
        
        // itemId가 있으면 사용
        if (paymentBase.getItemId() != null) {
            return "item_" + paymentBase.getItemId();
        }
        
        // 모든 식별자가 없으면 개별 키 사용 (중복 제거 불가)
        return "unknown_" + paymentDetail.getId();
    }
}

