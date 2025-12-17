package com.api.payment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.api.payment.domain.PaymentBase;
import com.api.payment.domain.PaymentDetail;
import com.api.payment.domain.PaymentHistory;

@Mapper  // MyBatis Mapper 인터페이스로 등록
public interface PaymentMapper {

    // ========== payment_base 관련 ==========
    
    // 입찰 기본 정보 생성
    void insertPaymentBase(PaymentBase paymentBase);
    
    // 입찰 기본 정보 조회 (ID)
    PaymentBase selectPaymentBaseById(@Param("id") Long id);
    
    // 회원과 item에 대한 기존 payment_base 조회 (중복 체크용)
    PaymentBase selectPaymentBaseByMemberAndItem(
            @Param("memberId") String memberId,
            @Param("itemId") Long itemId);
    
    // 회원의 입찰 내역 조회 (payment_base 목록)
    List<PaymentBase> selectPaymentBasesByMemberId(@Param("memberId") String memberId);
    
    // 입찰 기본 정보 삭제
    void deletePaymentBase(@Param("id") Long id);
    
    // ========== payment_detail 관련 ==========
    
    // 결제 상세 정보 생성
    void insertPaymentDetail(PaymentDetail paymentDetail);
    
    // 결제 상세 정보 조회 (ID)
    PaymentDetail selectPaymentDetailById(@Param("id") Long id);
    
    // 결제 상세 정보 조회 (payment_base.id로)
    PaymentDetail selectPaymentDetailByPaymentId(@Param("paymentId") Long paymentId);
    
    // 결제 상세 정보 조회 (merchant_uid)
    PaymentDetail selectPaymentDetailByMerchantUid(@Param("merchantUid") String merchantUid);
    
    // 결제 상세 정보 조회 (imp_uid)
    PaymentDetail selectPaymentDetailByImpUid(@Param("impUid") String impUid);
    
    // 결제 상세 정보 업데이트
    void updatePaymentDetail(PaymentDetail paymentDetail);
    
    // 결제 상태 업데이트
    void updatePaymentDetailStatus(@Param("id") Long id, @Param("status") String status);
    
    // 결제 상세 정보 삭제
    void deletePaymentDetail(@Param("id") Long id);
    
    // 회원의 결제 내역 조회 (payment_base와 payment_detail 조인)
    List<PaymentDetail> selectPaymentDetailsByMemberId(@Param("memberId") String memberId);
    
    // ========== payment_history 관련 ==========
    
    // 결제 히스토리 추가
    void insertPaymentHistory(PaymentHistory history);
    
    // 결제 히스토리 조회 (payment_detail.id로)
    List<PaymentHistory> selectPaymentHistoryByPaymentId(@Param("paymentId") Long paymentId);
}


