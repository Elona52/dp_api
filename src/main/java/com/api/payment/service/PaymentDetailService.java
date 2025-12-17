package com.api.payment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.favorite.service.ServiceResponse;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.payment.domain.PaymentBase;
import com.api.payment.domain.PaymentDetail;
import com.api.payment.domain.PaymentHistory;
import com.api.payment.dto.PaymentConverter;
import com.api.payment.dto.PaymentResponse;
import com.api.payment.util.PaymentKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 상세 정보 조회 및 관리 서비스
 * 구체적인 결제 처리 및 입찰서 작성 비즈니스 로직 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentDetailService {

    private final PaymentService paymentService;
    private final ItemRestService itemService;
    private final com.api.member.service.MemberService memberService;
    private final PaymentConverter paymentConverter;
    private final PaymentKeyGenerator paymentKeyGenerator;
    // 결제 정보를 메모리에 임시 저장 (결제 완료 전까지) - 호환성을 위해 유지
    private final Map<String, PaymentDetail> paymentStore = new ConcurrentHashMap<>();

    /**
     * 결제 상세 정보 조회 (payment_detail.id로) - DB + 메모리 조회
     * 
     * @param paymentDetailId payment_detail.id (결제 상세 ID)
     * @return PaymentDetail 객체
     */
    public PaymentDetail getPayment(Long paymentDetailId) {
        // DB에서 조회 (payment_detail)
        PaymentDetail paymentDetail = paymentService.selectPaymentDetailById(paymentDetailId);
        if (paymentDetail != null) {
            return paymentDetail;
        }
        
        // DB에 없으면 메모리에서 조회
        return paymentStore.values().stream()
                .filter(p -> p.getId().equals(paymentDetailId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 주문번호로 결제 상세 정보 조회 - DB + 메모리 조회
     * 
     * @param merchantUid 가맹점 주문번호
     * @return PaymentDetail 객체
     */
    public PaymentDetail getPaymentByMerchantUid(String merchantUid) {
        // DB에서 조회
        PaymentDetail paymentDetail = paymentService.selectPaymentDetailByMerchantUid(merchantUid);
        if (paymentDetail != null) {
            return paymentDetail;
        }
        
        // DB에 없으면 메모리에서 조회
        return paymentStore.get(merchantUid);
    }

    
    /**
     * PaymentBase와 PaymentDetail로부터 ItemDetail 조회 (헬퍼 메서드)
     */
    public ItemDetail getItemDetail(PaymentBase paymentBase, PaymentDetail paymentDetail) {
        if (paymentBase != null && paymentBase.getItemId() != null) {
            return itemService.getItemDetail(paymentBase.getItemId());
        } else if (paymentDetail != null && paymentDetail.getCltrNo() != null) {
            return itemService.getItemDetailByCltrMnmtNo(paymentDetail.getCltrNo());
        }
        return null;
    }
    
    /**
     * PaymentResponse에 deadlineDate 설정 (ItemDetail의 날짜 파싱 포함)
     */
    public void setDeadlineDateForPaymentResponse(PaymentResponse response, PaymentDetail paymentDetail, ItemDetail itemDetail) {
        try {
            if (itemDetail != null && itemDetail.getBidEnd() != null) {
                java.time.LocalDateTime bidEnd = itemDetail.getBidEnd();
                response.setDeadlineDate(Timestamp.valueOf(bidEnd));
            } else {
                // payment_base에서 itemId 조회
                PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
                if (paymentBase != null) {
                    ItemDetail detail = getItemDetail(paymentBase, paymentDetail);
                    if (detail != null && detail.getBidEnd() != null) {
                        java.time.LocalDateTime bidEnd = detail.getBidEnd();
                        response.setDeadlineDate(Timestamp.valueOf(bidEnd));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("입찰 마감일시 조회 실패: paymentDetailId={}, error={}", paymentDetail.getId(), e.getMessage());
        }
    }

    /**
     * 결제 상세 페이지 데이터 준비
     */
    public ServiceResponse<Map<String, Object>> handlePaymentDetailPageRequest(
            String memberId, Long paymentId) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                data.put("redirect", "/memberLogin");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
            }
            
            PaymentDetail paymentDetail = getPayment(paymentId);
            
            if (paymentDetail == null) {
                data.put("error", "결제 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            // payment_base에서 memberId 확인
            PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
            if (paymentBase == null || !memberId.equals(paymentBase.getMemberId())) {
                data.put("error", "권한이 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            // ItemDetail 정보 조회
            ItemDetail itemDetail = getItemDetail(paymentBase, paymentDetail);
            
            PaymentResponse paymentResponse = paymentConverter.toResponse(paymentDetail, paymentBase);
            setDeadlineDateForPaymentResponse(paymentResponse, paymentDetail, itemDetail);
            
            data.put("payment", paymentResponse);
            data.put("itemDetail", itemDetail);
            data.put("pageType", "detail");
            
            return ServiceResponse.ok(data);
        } catch (Exception e) {
            log.error("결제 상세 페이지 데이터 준비 중 오류 발생", e);
            data.put("error", "결제 상세 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    /**
     * 메모리 저장소에 PaymentDetail 저장 (결제 완료 전까지)
     */
    public void putPaymentStore(String merchantUid, PaymentDetail paymentDetail) {
        paymentStore.put(merchantUid, paymentDetail);
    }

    /**
     * 메모리 저장소에서 PaymentDetail 조회
     */
    public PaymentDetail getPaymentStore(String merchantUid) {
        return paymentStore.get(merchantUid);
    }

    /**
     * 내 결제 내역 페이지 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleMyPaymentsPageRequest(String memberId) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                data.put("redirect", "/memberLogin");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
            }
            
            List<PaymentResponse> paymentResponses = prepareMyPaymentsData(memberId);
            data.put("payments", paymentResponses);
            data.put("pageType", "list");
            return ServiceResponse.ok(data);
        } catch (Exception e) {
            log.error("내 결제 내역 페이지 데이터 준비 중 오류 발생", e);
            data.put("error", "내 결제 내역을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    /**
     * 내 결제 내역 데이터 준비 (중복 제거 및 정보 조회 포함)
     */
    private List<PaymentResponse> prepareMyPaymentsData(String memberId) {
        try {
            List<PaymentDetail> paymentDetails = paymentService.selectPaymentDetailsByMemberId(memberId);
            
            // 중복 제거: 같은 itemId/cltrNo에 대한 payment 중 가장 최근 것만 유지
            // itemId와 cltrNo는 같은 물건을 나타낼 수 있으므로 통합 처리
            Map<String, PaymentDetail> uniquePayments = new LinkedHashMap<>();
            for (PaymentDetail paymentDetail : paymentDetails) {
                // payment_base에서 itemId 조회
                PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
                if (paymentBase == null) continue;
                
                String key = paymentKeyGenerator.generateKey(paymentDetail, paymentBase);
                
                // itemId만 있고 cltrNo가 없는 경우, ItemDetail을 조회하여 cltrNo 기반 키로 변환
                if (paymentBase.getItemId() != null && (paymentDetail.getCltrNo() == null || paymentDetail.getCltrNo().isEmpty())) {
                    try {
                        ItemDetail itemDetail = itemService.getItemDetail(paymentBase.getItemId());
                        if (itemDetail != null && itemDetail.getCltrMnmtNo() != null && !itemDetail.getCltrMnmtNo().isEmpty()) {
                            // cltrNo 기반 키로 변경 (더 고유함)
                            key = "cltr_" + itemDetail.getCltrMnmtNo();
                        }
                    } catch (Exception e) {
                        log.warn("ItemDetail 조회 실패로 인한 키 변환 실패: itemId={}, error={}", 
                                paymentBase.getItemId(), e.getMessage());
                    }
                }
                
                // 중복 제거: 같은 키가 없거나, 현재 payment가 더 최근이면 업데이트
                if (!uniquePayments.containsKey(key)) {
                    uniquePayments.put(key, paymentDetail);
                } else {
                    PaymentDetail existing = uniquePayments.get(key);
                    // createdAt null 체크 및 비교
                    Timestamp currentDate = paymentDetail.getCreatedAt();
                    Timestamp existingDate = existing.getCreatedAt();
                    
                    if (currentDate != null && existingDate != null) {
                        if (currentDate.after(existingDate)) {
                            uniquePayments.put(key, paymentDetail);
                        }
                    } else if (currentDate != null && existingDate == null) {
                        // 현재 payment에만 날짜가 있으면 현재 것으로 교체
                        uniquePayments.put(key, paymentDetail);
                    }
                    // 둘 다 null이거나 existing만 있으면 기존 것 유지
                }
            }
            
            // Domain 리스트를 DTO 리스트로 변환
            return uniquePayments.values().stream()
                    .map(paymentDetail -> {
                        // payment_base 조회
                        PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
                        if (paymentBase == null) {
                            log.warn("payment_base를 찾을 수 없음: paymentId={}", paymentDetail.getPaymentId());
                            return null;
                        }
                        
                        // PaymentDetail과 PaymentBase를 결합하여 PaymentResponse 생성
                        PaymentResponse response = paymentConverter.toResponse(paymentDetail, paymentBase);
                        
                        // 각 Payment에 대한 ItemDetail 정보 조회하여 deadlineDate와 itemName 설정
                        try {
                            if (paymentBase.getItemId() != null) {
                                ItemDetail itemDetail = itemService.getItemDetail(paymentBase.getItemId());
                                if (itemDetail != null) {
                                    if (itemDetail.getBidEnd() != null) {
                                        java.time.LocalDateTime bidEnd = itemDetail.getBidEnd();
                                        response.setDeadlineDate(Timestamp.valueOf(bidEnd));
                                    }
                                    if (response.getItemName() == null || response.getItemName().isEmpty()) {
                                        response.setItemName(itemDetail.getAddress() != null ? itemDetail.getAddress() : itemDetail.getGoodsDetail());
                                    }
                                    if (response.getCltrNo() == null || response.getCltrNo().isEmpty()) {
                                        response.setCltrNo(itemDetail.getCltrMnmtNo());
                                    }
                                }
                            } else if (paymentDetail.getCltrNo() != null) {
                                ItemDetail itemDetail = itemService.getItemDetailByCltrMnmtNo(paymentDetail.getCltrNo());
                                if (itemDetail != null) {
                                    if (itemDetail.getBidEnd() != null) {
                                        java.time.LocalDateTime bidEnd = itemDetail.getBidEnd();
                                        response.setDeadlineDate(Timestamp.valueOf(bidEnd));
                                    }
                                    if (response.getItemName() == null || response.getItemName().isEmpty()) {
                                        response.setItemName(itemDetail.getAddress() != null ? itemDetail.getAddress() : itemDetail.getGoodsDetail());
                                    }
                                    if (response.getItemId() == null) {
                                        response.setItemId(itemDetail.getPlnmNo());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("입찰 정보 조회 실패: paymentDetailId={}, error={}", paymentDetail.getId(), e.getMessage());
                        }
                        response.formatDates();
                        return response;
                    })
                    .filter(response -> response != null)
                        .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("내 결제 내역 데이터 준비 중 오류 발생", e);
            return new ArrayList<>();
        }
    }

    // ========== 결제 처리 비즈니스 로직 ==========

    /**
     * 결제 준비 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handlePreparePaymentRequest(
            String memberId,
            Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (memberId == null || memberId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            if (requestBody == null || !requestBody.containsKey("amount")) {
                response.put("success", false);
                response.put("message", "amount가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            Long itemId = requestBody.get("itemId") != null ? Long.parseLong(requestBody.get("itemId").toString()) : null;
            String cltrNo = requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").toString() : null;
            Long amount = Long.parseLong(requestBody.get("amount").toString());
            String itemName = requestBody.get("itemName") != null ? requestBody.get("itemName").toString() : "상품";

            PaymentDetail paymentDetail = preparePayment(null, itemId, cltrNo, memberId, amount, itemName);
            response.put("success", true);
            response.put("merchantUid", paymentDetail.getMerchantUid());
            response.put("amount", paymentDetail.getAmount());
            return ServiceResponse.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        } catch (Exception e) {
            log.error("결제 준비 중 예상치 못한 오류", e);
            response.put("success", false);
            response.put("message", "결제 준비 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 결제 준비 (주문번호 생성 및 결제 정보 초기 저장)
     */
    @Transactional
    public PaymentDetail preparePayment(Integer auctionNo, Long itemId, String cltrNo, String memberId, Long amount, String itemName) {
        // 입력값 검증
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }
        if (itemId == null && (cltrNo == null || cltrNo.trim().isEmpty())) {
            throw new IllegalArgumentException("아이템 정보(itemId 또는 cltrNo)가 필요합니다.");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("결제 금액이 올바르지 않습니다.");
        }
        
        // itemId와 cltrNo 중 하나만 있어도 다른 하나를 자동으로 채워줌
        Long finalItemId = itemId;
        String finalCltrNo = cltrNo;
        
        // itemId만 있고 cltrNo가 없는 경우, ItemDetail을 조회하여 cltrNo 채우기
        if (itemId != null && (cltrNo == null || cltrNo.isEmpty())) {
            try {
                ItemDetail itemDetail = itemService.getItemDetail(itemId);
                if (itemDetail != null && itemDetail.getCltrMnmtNo() != null && !itemDetail.getCltrMnmtNo().isEmpty()) {
                    finalCltrNo = itemDetail.getCltrMnmtNo();
                    log.info("itemId로부터 cltrNo 자동 채움 - itemId={}, cltrNo={}", itemId, finalCltrNo);
                }
            } catch (Exception e) {
                log.warn("itemId로부터 cltrNo 조회 실패: itemId={}, error={}", itemId, e.getMessage());
            }
        }
        
        // cltrNo만 있고 itemId가 없는 경우, ItemDetail을 조회하여 itemId 채우기
        if ((itemId == null) && (cltrNo != null && !cltrNo.isEmpty())) {
            try {
                ItemDetail itemDetail = itemService.getItemDetailByCltrMnmtNo(cltrNo);
                if (itemDetail != null && itemDetail.getPlnmNo() != null) {
                    finalItemId = itemDetail.getPlnmNo();
                    log.info("cltrNo로부터 itemId 자동 채움 - cltrNo={}, itemId={}", cltrNo, finalItemId);
                }
            } catch (Exception e) {
                log.warn("cltrNo로부터 itemId 조회 실패: cltrNo={}, error={}", cltrNo, e.getMessage());
            }
        }
        
        log.info("Payment 생성 시작 - itemId={}, cltrNo={}, memberId={}, amount={}", finalItemId, finalCltrNo, memberId, amount);

        try {
            // 1. payment_base 생성 (회원ID + 물건번호 기준)
            PaymentBase paymentBase = new PaymentBase();
            paymentBase.setMemberId(memberId);
            paymentBase.setItemId(finalItemId);
            paymentBase.setBidPrice(amount);
            paymentBase.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            
            paymentService.insertPaymentBase(paymentBase);
            log.info("payment_base 생성 성공: id={}, memberId={}, itemId={}, bidPrice={}", 
                paymentBase.getId(), memberId, finalItemId, amount);
            
            // 2. payment_detail 생성 (payment_base.id를 payment_id로 참조)
            PaymentDetail paymentDetail = new PaymentDetail();
            paymentDetail.setPaymentId(paymentBase.getId());
            paymentDetail.setCltrNo(finalCltrNo);
            paymentDetail.setAmount(amount);
            paymentDetail.setItemName(itemName != null ? itemName : "상품");
            paymentDetail.setStatus("ready");
            paymentDetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            paymentDetail.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

            String merchantUid = "merchant_" + System.currentTimeMillis() + "_" + memberId;
            paymentDetail.setMerchantUid(merchantUid);
            
            paymentService.insertPaymentDetail(paymentDetail);
            log.info("payment_detail 생성 성공: id={}, paymentId(payment_base.id)={}, merchantUid={}", 
                paymentDetail.getId(), paymentBase.getId(), merchantUid);
            
            // 메모리에도 임시 저장 (결제 완료 전까지)
            putPaymentStore(merchantUid, paymentDetail);
            
            return paymentDetail;
        } catch (Exception e) {
            log.error("결제 준비 실패: memberId={}, itemId={}, cltrNo={}, error={}", 
                memberId, finalItemId, finalCltrNo, e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                throw new RuntimeException("회원 정보 또는 물건 정보를 확인할 수 없습니다.", e);
            }
            throw new RuntimeException("결제 준비에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 처리 요청
     */
    public ServiceResponse<Map<String, Object>> handleCompletePaymentRequest(String impUid, String merchantUid) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (merchantUid == null || merchantUid.isEmpty()) {
                response.put("success", false);
                response.put("message", "merchant_uid가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            Map<String, Object> result = completePayment(impUid, merchantUid);

            if (Boolean.TRUE.equals(result.get("success"))) {
                PaymentDetail paymentDetail = (PaymentDetail) result.get("payment");
                if (paymentDetail != null) {
                    PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
                    if (paymentBase != null) {
                        log.info("결제 완료 처리 - paymentDetailId: {}, paymentBaseId: {}, itemId: {}, cltrNo: {}", 
                            paymentDetail.getId(), paymentBase.getId(), paymentBase.getItemId(), paymentDetail.getCltrNo());
                    }
                }
            }

            return ServiceResponse.ok(result);

        } catch (Exception e) {
            log.error("결제 완료 처리 중 오류", e);
            response.put("success", false);
            response.put("message", "결제 완료 처리 중 오류가 발생했습니다.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 결제 완료 처리 (아임포트 콜백)
     */
    @Transactional
    public Map<String, Object> completePayment(String impUid, String merchantUid) {
        Map<String, Object> result = new HashMap<>();
        
        PaymentDetail paymentDetail = getPaymentByMerchantUid(merchantUid);

        if (paymentDetail == null) {
            result.put("success", false);
            result.put("message", "결제 정보가 존재하지 않습니다.");
            return result;
        }

        paymentDetail.setImpUid(impUid != null ? impUid : "imp_" + System.currentTimeMillis());
        paymentDetail.setStatus("paid");
        paymentDetail.setPaidAt(new Timestamp(System.currentTimeMillis()));
        paymentDetail.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // DB에 업데이트
        paymentService.updatePaymentDetail(paymentDetail);
        
        // 결제 히스토리 추가
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(paymentDetail.getId());
        history.setStatus("paid");
        history.setAction("결제 완료");
        history.setDescription("결제가 완료되었습니다. imp_uid: " + impUid);
        paymentService.insertPaymentHistory(history);

        result.put("success", true);
        result.put("payment", paymentDetail);
        return result;
    }

    /**
     * 결제 취소 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleCancelPaymentRequest(
            String memberId,
            Long paymentId,
            String reason) {

        Map<String, Object> response = new HashMap<>();

        try {
            if (memberId == null || memberId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            if (paymentId == null) {
                response.put("success", false);
                response.put("message", "paymentId가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            PaymentDetail paymentDetail = getPayment(paymentId);
            if (paymentDetail == null) {
                response.put("success", false);
                response.put("message", "결제 정보를 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }

            PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
            if (paymentBase == null || !memberId.equals(paymentBase.getMemberId())) {
                response.put("success", false);
                response.put("message", "권한이 없습니다.");
                return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
            }

            String cancelReason = (reason == null || reason.trim().isEmpty()) ? "구매자 요청" : reason;
            boolean success = cancelPayment(paymentId, cancelReason);

            if (success) {
                log.info("결제 취소 처리 - paymentDetailId: {}, paymentBaseId: {}, itemId: {}, cltrNo: {}", 
                    paymentId, paymentBase.getId(), paymentBase.getItemId(), paymentDetail.getCltrNo());
                response.put("success", true);
                response.put("message", "결제가 취소되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "결제 취소에 실패했습니다.");
            }

            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("결제 취소 중 오류", e);
            response.put("success", false);
            response.put("message", "결제 취소 중 오류가 발생했습니다.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 결제 취소
     */
    @Transactional
    public boolean cancelPayment(Long paymentDetailId, String reason) {
        PaymentDetail paymentDetail = getPayment(paymentDetailId);
        if (paymentDetail == null) return false;

        paymentDetail.setStatus("cancelled");
        paymentDetail.setCancelReason(reason);
        paymentDetail.setCancelledAt(new Timestamp(System.currentTimeMillis()));
        paymentDetail.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        // DB에 업데이트
        paymentService.updatePaymentDetail(paymentDetail);
        
        // 결제 히스토리 추가
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(paymentDetailId);
        history.setStatus("cancelled");
        history.setAction("결제 취소");
        history.setDescription("결제가 취소되었습니다. 사유: " + reason);
        paymentService.insertPaymentHistory(history);
        
        return true;
    }


    /**
     * 입찰내역 삭제
     */
    @Transactional
    public boolean deletePayment(Long paymentDetailId) {
        try {
            PaymentDetail paymentDetail = getPayment(paymentDetailId);
            if (paymentDetail == null) {
                log.warn("삭제할 PaymentDetail을 찾을 수 없음: paymentDetailId={}", paymentDetailId);
                return false;
            }
            
            // payment_base 삭제 (CASCADE로 payment_detail도 자동 삭제됨)
            paymentService.deletePaymentBase(paymentDetail.getPaymentId());
            log.info("Payment 삭제 완료: paymentDetailId={}, paymentBaseId={}", paymentDetailId, paymentDetail.getPaymentId());
            return true;
        } catch (Exception e) {
            log.error("Payment 삭제 중 오류 발생: paymentDetailId={}", paymentDetailId, e);
            return false;
        }
    }

    /**
     * 입찰내역 삭제 처리
     */
    public ServiceResponse<Map<String, Object>> handleDeletePaymentRequest(
            String memberId, Long paymentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }
            
            PaymentDetail paymentDetail = getPayment(paymentId);
            if (paymentDetail == null) {
                response.put("success", false);
                response.put("message", "입찰내역을 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }
            
            PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
            if (paymentBase == null) {
                response.put("success", false);
                response.put("message", "입찰 기본 정보를 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }
            
            if (!memberId.equals(paymentBase.getMemberId())) {
                response.put("success", false);
                response.put("message", "본인의 입찰내역만 삭제할 수 있습니다.");
                return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
            }
            
            if ("paid".equals(paymentDetail.getStatus())) {
                response.put("success", false);
                response.put("message", "결제가 완료된 입찰내역은 삭제할 수 없습니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }
            
            boolean deleted = deletePayment(paymentId);
            if (deleted) {
                log.info("입찰내역 삭제 성공 - paymentDetailId: {}, paymentBaseId: {}, memberId: {}", 
                    paymentId, paymentBase.getId(), memberId);
                response.put("success", true);
                response.put("message", "입찰내역이 삭제되었습니다.");
                return ServiceResponse.ok(response);
            } else {
                log.error("입찰내역 삭제 실패 - paymentDetailId: {}, memberId: {}", paymentId, memberId);
                response.put("success", false);
                response.put("message", "입찰내역 삭제에 실패했습니다.");
                return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
            }
        } catch (Exception e) {
            log.error("입찰내역 삭제 중 오류 발생 - paymentId: {}, memberId: {}", paymentId, memberId, e);
            response.put("success", false);
            response.put("message", "입찰내역 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    // ========== 입찰서 작성 비즈니스 로직 ==========

    /**
     * 입찰서 작성 페이지 데이터 준비
     */
    public ServiceResponse<Map<String, Object>> handleBidFormPageRequest(
            String memberId, Long itemId, String cltrNo) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                data.put("error", "로그인이 필요합니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
            }
            
            PaymentBase existingPaymentBase = paymentService.selectPaymentBaseByMemberAndItem(memberId, itemId);
            if (existingPaymentBase != null) {
                PaymentDetail existingPaymentDetail = paymentService.selectPaymentDetailByPaymentId(existingPaymentBase.getId());
                if (existingPaymentDetail != null) {
                    log.warn("이미 작성된 입찰서 존재 - paymentDetailId: {}, paymentBaseId: {}, memberId: {}", 
                        existingPaymentDetail.getId(), existingPaymentBase.getId(), memberId);
                    data.put("error", "이미 입찰서가 작성되었습니다.");
                    data.put("existingPaymentId", existingPaymentDetail.getId());
                    return ServiceResponse.ok(data);
                }
            }
            
            Map<String, Object> checkoutData = prepareCheckoutPageData(memberId, null, itemId, cltrNo);
            if (checkoutData.containsKey("error")) {
                log.error("입찰서 작성 페이지 데이터 준비 실패: {}", checkoutData.get("error"));
                return ServiceResponse.ok(checkoutData);
            }
            
            return ServiceResponse.ok(checkoutData);
        } catch (Exception e) {
            log.error("입찰서 작성 페이지 오류 발생", e);
            data.put("error", "입찰서 작성 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    /**
     * 입찰서 제출 처리
     */
    public ServiceResponse<Map<String, Object>> handleSubmitBidRequest(
            String memberId, Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }
            
            log.info("입찰서 제출 요청 - memberId: {}, requestBody: {}", memberId, requestBody);
            
            Long itemId = requestBody.get("itemId") != null ? Long.parseLong(requestBody.get("itemId").toString()) : null;
            String cltrNo = requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").toString() : null;
            Long bidAmount = requestBody.get("bidAmount") != null ? Long.parseLong(requestBody.get("bidAmount").toString()) : null;
            Long depositAmount = requestBody.get("depositAmount") != null ? Long.parseLong(requestBody.get("depositAmount").toString()) : null;
            String itemName = requestBody.get("itemName") != null ? requestBody.get("itemName").toString() : "상품";
            
            log.info("입찰서 제출 파라미터 - itemId: {}, cltrNo: {}, bidAmount: {}, depositAmount: {}, itemName: {}", 
                    itemId, cltrNo, bidAmount, depositAmount, itemName);
            
            PaymentBase existingPaymentBase = paymentService.selectPaymentBaseByMemberAndItem(memberId, itemId);
            if (existingPaymentBase != null) {
                PaymentDetail existingPaymentDetail = paymentService.selectPaymentDetailByPaymentId(existingPaymentBase.getId());
                if (existingPaymentDetail != null) {
                    log.warn("입찰서 제출 실패 - 이미 작성된 입찰서 존재: paymentDetailId={}, paymentBaseId={}, memberId={}", 
                        existingPaymentDetail.getId(), existingPaymentBase.getId(), memberId);
                    response.put("success", false);
                    response.put("message", "이미 입찰서가 작성되었습니다.");
                    response.put("existingPaymentId", existingPaymentDetail.getId());
                    return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
                }
            }
            
            if (depositAmount == null || depositAmount <= 0) {
                response.put("success", false);
                response.put("message", "보증금액이 올바르지 않습니다.");
                log.warn("입찰서 제출 실패 - 보증금액이 올바르지 않음: {}", depositAmount);
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }
            
            PaymentDetail paymentDetail = preparePayment(null, itemId, cltrNo, memberId, depositAmount, itemName);
            
            if (paymentDetail == null || paymentDetail.getId() == null) {
                log.error("입찰서 제출 실패 - PaymentDetail 생성 실패");
                response.put("success", false);
                response.put("message", "입찰서 제출에 실패했습니다. PaymentDetail 생성에 실패했습니다.");
                return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
            }
            
            log.info("입찰서 제출 성공 - paymentDetailId: {}, paymentBaseId: {}, merchantUid: {}", 
                paymentDetail.getId(), paymentDetail.getPaymentId(), paymentDetail.getMerchantUid());
            
            response.put("success", true);
            response.put("paymentId", paymentDetail.getId());
            response.put("merchantUid", paymentDetail.getMerchantUid());
            response.put("bidAmount", bidAmount);
            response.put("depositAmount", depositAmount);
            return ServiceResponse.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("입찰서 제출 중 숫자 형식 오류", e);
            response.put("success", false);
            response.put("message", "입력값 형식이 올바르지 않습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
        } catch (IllegalArgumentException e) {
            log.error("입찰서 제출 중 유효성 검사 오류", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
        } catch (Exception e) {
            log.error("입찰서 제출 중 예상치 못한 오류", e);
            response.put("success", false);
            response.put("message", "입찰서 제출 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 입찰서 제출 완료 페이지 데이터 준비
     */
    public ServiceResponse<Map<String, Object>> handleBidSubmittedPageRequest(
            String memberId, Long paymentId, Long bidAmount, Long depositAmount,
            String bidMethod, String paymentMethod, String refundBank,
            String refundAccountNumber, String refundAccountHolder) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (paymentId == null) {
                log.error("입찰서 제출 완료 페이지 접근 - paymentId가 없음");
                data.put("error", "입찰서 정보를 찾을 수 없습니다. paymentId가 필요합니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            PaymentDetail paymentDetail = getPayment(paymentId);
            if (paymentDetail == null) {
                log.error("입찰서 제출 완료 페이지 접근 - PaymentDetail을 찾을 수 없음: paymentId={}", paymentId);
                data.put("error", "입찰서 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
            if (paymentBase == null) {
                log.error("입찰서 제출 완료 페이지 접근 - PaymentBase를 찾을 수 없음: paymentBaseId={}", paymentDetail.getPaymentId());
                data.put("error", "입찰서 기본 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            String paymentMemberId = paymentBase.getMemberId();
            if (memberId == null || memberId.isEmpty()) {
                data.put("redirect", "/memberLogin");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
            }
            
            if (!memberId.equals(paymentMemberId)) {
                data.put("error", "권한이 없습니다.");
                return ServiceResponse.ok(data);
            }
            
            if (depositAmount == null && paymentDetail.getAmount() != null) {
                depositAmount = paymentDetail.getAmount();
            }
            
            if (bidAmount != null) data.put("bidAmount", bidAmount);
            if (depositAmount != null) data.put("depositAmount", depositAmount);
            if (bidMethod != null) data.put("bidMethod", bidMethod);
            if (paymentMethod != null) data.put("paymentMethod", paymentMethod);
            if (refundBank != null) data.put("refundBank", refundBank);
            if (refundAccountNumber != null) data.put("refundAccountNumber", refundAccountNumber);
            if (refundAccountHolder != null) data.put("refundAccountHolder", refundAccountHolder);
            
            ItemDetail itemDetail = getItemDetail(paymentBase, paymentDetail);
            
            if (itemDetail != null) {
                data.put("itemDetail", itemDetail);
            }
            
            com.api.member.domain.Member member = null;
            if (memberId != null) {
                member = memberService.getMemberInfo(memberId);
            }
            
            PaymentResponse paymentResponse = paymentConverter.toResponse(paymentDetail, paymentBase);
            setDeadlineDateForPaymentResponse(paymentResponse, paymentDetail, itemDetail);
            
            data.put("payment", paymentResponse);
            data.put("itemDetail", itemDetail);
            data.put("member", member);
            data.put("buyerName", paymentDetail.getBuyerName());
            data.put("buyerEmail", paymentDetail.getBuyerEmail());
            data.put("buyerPhone", paymentDetail.getBuyerTel());
            
            return ServiceResponse.ok(data);
        } catch (Exception e) {
            log.error("입찰서 제출 완료 페이지 데이터 준비 중 오류 발생", e);
            data.put("error", "입찰서 제출 완료 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    // ========== 결제 페이지 데이터 준비 ==========

    /**
     * 결제 페이지 데이터 준비
     */
    public Map<String, Object> prepareCheckoutPageData(String memberId, Integer auctionNo, Long itemId, String cltrNo) {
        Map<String, Object> data = new HashMap<>();
        
        com.api.member.domain.Member member = memberService.getMemberInfo(memberId);
        if (member == null) {
            data.put("error", "회원 정보를 찾을 수 없습니다.");
            data.put("pageType", "fail");
            return data;
        }
        data.put("member", member);
        
        ItemDetail itemDetail = null;
        
        if (itemId != null) {
            itemDetail = itemService.getItemDetail(itemId);
            
            if (itemDetail == null) {
                data.put("error", "물건 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return data;
            }
            
            data.put("itemId", itemId);
            if (itemDetail.getCltrMnmtNo() != null) {
                data.put("cltrNo", itemDetail.getCltrMnmtNo());
            }
        } else if (cltrNo != null && !cltrNo.isEmpty()) {
            itemDetail = itemService.getItemDetailByCltrMnmtNo(cltrNo);
            
            if (itemDetail == null) {
                data.put("error", "물건 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return data;
            }
            
            if (itemDetail.getPlnmNo() != null) {
                data.put("itemId", itemDetail.getPlnmNo());
            }
            data.put("cltrNo", cltrNo);
        } else {
            data.put("error", "물건 번호(itemId 또는 cltrNo)를 제공해주세요.");
            data.put("pageType", "fail");
            return data;
        }
        
        if (itemId != null || cltrNo != null) {
            PaymentBase existingPaymentBase = paymentService.selectPaymentBaseByMemberAndItem(memberId, itemId);
            if (existingPaymentBase != null) {
                PaymentDetail existingPaymentDetail = paymentService.selectPaymentDetailByPaymentId(existingPaymentBase.getId());
                if (existingPaymentDetail != null && "paid".equals(existingPaymentDetail.getStatus())) {
                    data.put("error", "이미 결제가 완료된 물건입니다.");
                    data.put("pageType", "fail");
                    return data;
                }
            }
        }
        
        data.put("itemDetail", itemDetail);
        Long amount = itemDetail.getMinBidPriceMin() != null && itemDetail.getMinBidPriceMin() > 0 
            ? itemDetail.getMinBidPriceMin() 
            : (itemDetail.getAppraisalAmountMin() != null ? itemDetail.getAppraisalAmountMin() : 0L);
        data.put("amount", amount);
        data.put("pageType", "checkout");
        
        if (itemId != null && !data.containsKey("itemId")) {
            data.put("itemId", itemId);
        }
        if (cltrNo != null && !cltrNo.isEmpty() && !data.containsKey("cltrNo")) {
            data.put("cltrNo", cltrNo);
        }
        
        data.put("buyerName", member.getName() != null ? member.getName() : "");
        data.put("buyerEmail", member.getMail() != null ? member.getMail() : "");
        data.put("buyerPhone", member.getPhone() != null ? member.getPhone() : "");
        
        return data;
    }

    /**
     * 결제 페이지 데이터 준비 (paymentId 처리 포함)
     */
    public ServiceResponse<Map<String, Object>> handleCheckoutPageRequest(
            String memberId, Long paymentId, Long itemId, String cltrNo,
            Long bidAmount, Long depositAmount) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            if (memberId == null || memberId.isEmpty()) {
                data.put("redirect", "/memberLogin");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, data);
            }
            
            if (paymentId != null) {
                PaymentDetail paymentDetail = getPayment(paymentId);
                if (paymentDetail != null) {
                    PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
                    if (paymentBase != null && memberId.equals(paymentBase.getMemberId())) {
                        if (paymentBase.getItemId() != null) {
                            itemId = paymentBase.getItemId();
                        }
                        if (paymentDetail.getCltrNo() != null) {
                            cltrNo = paymentDetail.getCltrNo();
                        }
                        if (depositAmount == null && paymentDetail.getAmount() != null) {
                            depositAmount = paymentDetail.getAmount();
                        }
                        if (bidAmount == null && paymentBase.getBidPrice() != null) {
                            bidAmount = paymentBase.getBidPrice();
                        }
                    } else if (paymentBase != null && !memberId.equals(paymentBase.getMemberId())) {
                        data.put("error", "권한이 없습니다.");
                        return ServiceResponse.ok(data);
                    }
                }
            }
            
            Map<String, Object> checkoutData = prepareCheckoutPageData(memberId, null, itemId, cltrNo);
            if (checkoutData.containsKey("error")) {
                return ServiceResponse.ok(checkoutData);
            }
            
            checkoutData.put("bidAmount", bidAmount);
            checkoutData.put("depositAmount", depositAmount);
            if (depositAmount != null) {
                checkoutData.put("amount", depositAmount);
            }
            
            return ServiceResponse.ok(checkoutData);
        } catch (Exception e) {
            log.error("결제 페이지 데이터 준비 중 오류 발생", e);
            data.put("error", "결제 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    /**
     * 결제 성공 페이지 데이터 준비
     */
    public ServiceResponse<Map<String, Object>> handlePaymentSuccessPageRequest(
            String merchantUid, Long bidAmount, Long depositAmount,
            String bidMethod, String selectedBank) {
        Map<String, Object> data = new HashMap<>();
        
        try {
            PaymentDetail paymentDetail = getPaymentByMerchantUid(merchantUid);
            
            if (paymentDetail == null) {
                data.put("error", "결제 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            PaymentBase paymentBase = paymentService.getPaymentBase(paymentDetail);
            if (paymentBase == null) {
                data.put("error", "결제 기본 정보를 찾을 수 없습니다.");
                data.put("pageType", "fail");
                return ServiceResponse.ok(data);
            }
            
            if (bidAmount != null) data.put("bidAmount", bidAmount);
            if (depositAmount != null) data.put("depositAmount", depositAmount);
            if (bidMethod != null) data.put("bidMethod", bidMethod);
            if (selectedBank != null) data.put("selectedBank", selectedBank);
            
            ItemDetail itemDetail = getItemDetail(paymentBase, paymentDetail);
            
            String memberId = paymentBase.getMemberId();
            com.api.member.domain.Member member = null;
            if (memberId != null) {
                member = memberService.getMemberInfo(memberId);
            }
            
            PaymentResponse paymentResponse = paymentConverter.toResponse(paymentDetail, paymentBase);
            setDeadlineDateForPaymentResponse(paymentResponse, paymentDetail, itemDetail);
            
            data.put("payment", paymentResponse);
            data.put("itemDetail", itemDetail);
            data.put("member", member);
            data.put("buyerName", paymentDetail.getBuyerName());
            data.put("buyerEmail", paymentDetail.getBuyerEmail());
            data.put("buyerPhone", paymentDetail.getBuyerTel());
            data.put("pageType", "success");
            
            return ServiceResponse.ok(data);
        } catch (Exception e) {
            log.error("결제 성공 페이지 데이터 준비 중 오류 발생", e);
            data.put("error", "결제 성공 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, data);
        }
    }

    /**
     * 결제 실패 페이지 데이터 준비
     */
    public ServiceResponse<Map<String, Object>> handlePaymentFailPageRequest(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message != null ? message : "결제에 실패했습니다.");
        data.put("pageType", "fail");
        return ServiceResponse.ok(data);
    }
}

