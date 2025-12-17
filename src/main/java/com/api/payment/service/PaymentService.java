package com.api.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.payment.domain.PaymentBase;
import com.api.payment.domain.PaymentDetail;
import com.api.payment.domain.PaymentHistory;
import com.api.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 데이터베이스 CRUD 서비스
 * DB 저장 및 조회 작업 담당
 */
@Slf4j  
@Service  
@RequiredArgsConstructor 
@Transactional(readOnly = true) 
public class PaymentService {

    private final PaymentMapper paymentMapper;

    // ========== payment_base CRUD ==========
    
    /**
     * payment_base 생성
     */
    @Transactional
    public void insertPaymentBase(PaymentBase paymentBase) {
        paymentMapper.insertPaymentBase(paymentBase);
    }
    
    /**
     * payment_base 조회 (ID)
     */
    public PaymentBase selectPaymentBaseById(Long id) {
        return paymentMapper.selectPaymentBaseById(id);
    }
    
    /**
     * payment_base 조회 (회원ID + 물건번호)
     */
    public PaymentBase selectPaymentBaseByMemberAndItem(String memberId, Long itemId) {
        return paymentMapper.selectPaymentBaseByMemberAndItem(memberId, itemId);
    }
    
    /**
     * payment_base 목록 조회 (회원ID)
     */
    public List<PaymentBase> selectPaymentBasesByMemberId(String memberId) {
        return paymentMapper.selectPaymentBasesByMemberId(memberId);
    }
    
    /**
     * payment_base 삭제
     */
    @Transactional
    public void deletePaymentBase(Long id) {
        paymentMapper.deletePaymentBase(id);
    }

    // ========== payment_detail CRUD ==========
    
    /**
     * payment_detail 생성
     */
    @Transactional
    public void insertPaymentDetail(PaymentDetail paymentDetail) {
        paymentMapper.insertPaymentDetail(paymentDetail);
    }
    
    /**
     * payment_detail 조회 (ID)
     */
    public PaymentDetail selectPaymentDetailById(Long id) {
        return paymentMapper.selectPaymentDetailById(id);
    }
    
    /**
     * payment_detail 조회 (payment_base.id로)
     */
    public PaymentDetail selectPaymentDetailByPaymentId(Long paymentId) {
        return paymentMapper.selectPaymentDetailByPaymentId(paymentId);
    }
    
    /**
     * payment_detail 조회 (merchant_uid)
     */
    public PaymentDetail selectPaymentDetailByMerchantUid(String merchantUid) {
        return paymentMapper.selectPaymentDetailByMerchantUid(merchantUid);
    }
    
    /**
     * payment_detail 조회 (imp_uid)
     */
    public PaymentDetail selectPaymentDetailByImpUid(String impUid) {
        return paymentMapper.selectPaymentDetailByImpUid(impUid);
    }
    
    /**
     * payment_detail 업데이트
     */
    @Transactional
    public void updatePaymentDetail(PaymentDetail paymentDetail) {
        paymentMapper.updatePaymentDetail(paymentDetail);
    }
    
    /**
     * payment_detail 상태 업데이트
     */
    @Transactional
    public void updatePaymentDetailStatus(Long id, String status) {
        paymentMapper.updatePaymentDetailStatus(id, status);
    }
    
    /**
     * payment_detail 삭제
     */
    @Transactional
    public void deletePaymentDetail(Long id) {
        paymentMapper.deletePaymentDetail(id);
    }
    
    /**
     * payment_detail 목록 조회 (회원ID)
     */
    public List<PaymentDetail> selectPaymentDetailsByMemberId(String memberId) {
        return paymentMapper.selectPaymentDetailsByMemberId(memberId);
    }

    // ========== payment_history CRUD ==========
    
    /**
     * payment_history 생성
     */
    @Transactional
    public void insertPaymentHistory(PaymentHistory history) {
        paymentMapper.insertPaymentHistory(history);
    }
    
    /**
     * payment_history 목록 조회 (payment_detail.id로)
     */
    public List<PaymentHistory> selectPaymentHistoryByPaymentId(Long paymentId) {
        return paymentMapper.selectPaymentHistoryByPaymentId(paymentId);
    }

    // ========== 조회 헬퍼 메서드 ==========
    
    /**
     * PaymentDetail로부터 PaymentBase 조회
     * payment_base.id = payment_detail.payment_id 조인 관계 사용
     */
    public PaymentBase getPaymentBase(PaymentDetail paymentDetail) {
        if (paymentDetail == null) {
            return null;
        }
        return selectPaymentBaseById(paymentDetail.getPaymentId());
    }
}
