package com.api.payment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.api.config.IamportConfig;
import com.api.favorite.service.ServiceResponse;
import com.api.payment.service.PaymentDetailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@RequestMapping("/payment")
@Slf4j
public class PaymentController {

    private final PaymentDetailService paymentDetailService;
    private final IamportConfig iamportConfig;

    /**
     * 입찰서 작성 페이지로 이동
     * itemId 또는 cltrNo 중 하나는 필수 (auctionNo는 사용하지 않음)
     */
    @GetMapping("/bid-form")
    public String bidFormPage(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo,
            Model model) {
        
        ServiceResponse<Map<String, Object>> serviceResponse =
                paymentDetailService.handleBidFormPageRequest(memberId, itemId, cltrNo);
        Map<String, Object> data = serviceResponse.getData();
        
        return processServiceResponse(data, model, "payment/bid-form");
    }

    /**
     * 결제 페이지로 이동
     * paymentId가 있으면 Payment에서 정보를 가져오므로, itemId/cltrNo는 선택적
     * auctionNo는 사용하지 않음 (테이블에 없음)
     */
    @GetMapping("/checkout")
    public String checkoutPage(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @RequestParam(name = "paymentId", required = false) Long paymentId,
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handleCheckoutPageRequest(
                memberId, paymentId, itemId, cltrNo, bidAmount, depositAmount);
        Map<String, Object> data = serviceResponse.getData();
        
        String viewName = processServiceResponse(data, model, "payment/payment");
        if (!viewName.startsWith("redirect:")) {
            model.addAttribute("iamportImpCode", iamportConfig.getImpCode());
        }
        
        return viewName;
    }
    

    /**
     * 입찰서 제출 (결제 없이 입찰서만 제출)
     */
    @PostMapping("/submit-bid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitBid(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @RequestBody Map<String, Object> requestBody) {
        return paymentDetailService.handleSubmitBidRequest(memberId, requestBody).toResponseEntity();
    }

    /**
     * 결제 준비 (주문번호 생성)
     */
    @PostMapping("/prepare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> preparePayment(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @RequestBody Map<String, Object> requestBody) {
        return paymentDetailService.handlePreparePaymentRequest(memberId, requestBody).toResponseEntity();
    }

    /**
     * 결제 완료 (아임포트 콜백)
     */
    @PostMapping("/complete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> completePayment(@RequestBody Map<String, Object> requestBody) {
        String impUid = requestBody.get("imp_uid") != null ? requestBody.get("imp_uid").toString() : null;
        String merchantUid = requestBody.get("merchant_uid") != null ? requestBody.get("merchant_uid").toString() : null;
        log.info("결제 완료 콜백 - impUid: {}, merchantUid: {}", impUid, merchantUid);
        return paymentDetailService.handleCompletePaymentRequest(impUid, merchantUid).toResponseEntity();
    }

    /**
     * 입찰서 제출 완료 페이지
     * paymentId가 필수이며, Payment에서 memberId와 기본 정보를 가져옴
     * bidAmount, depositAmount 등은 입찰서 작성 시점 정보이므로 선택적
     */
    @GetMapping("/bid-submitted")
    public String bidSubmittedPage(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @RequestParam(name = "paymentId") Long paymentId,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            @RequestParam(name = "bidMethod", required = false) String bidMethod,
            @RequestParam(name = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(name = "refundBank", required = false) String refundBank,
            @RequestParam(name = "refundAccountNumber", required = false) String refundAccountNumber,
            @RequestParam(name = "refundAccountHolder", required = false) String refundAccountHolder,
            Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handleBidSubmittedPageRequest(
                memberId, paymentId, bidAmount, depositAmount, bidMethod, paymentMethod,
                refundBank, refundAccountNumber, refundAccountHolder);
        Map<String, Object> data = serviceResponse.getData();
        
        return processServiceResponse(data, model, "payment/bid-submitted");
    }

    /**
     * 결제 성공 페이지
     * merchantUid로 Payment를 찾아 memberId와 기본 정보를 가져옴
     */
    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam(name = "merchantUid") String merchantUid,
            @RequestParam(name = "bidAmount", required = false) Long bidAmount,
            @RequestParam(name = "depositAmount", required = false) Long depositAmount,
            @RequestParam(name = "bidMethod", required = false) String bidMethod,
            @RequestParam(name = "selectedBank", required = false) String selectedBank,
            Model model) {
        
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handlePaymentSuccessPageRequest(
                merchantUid, bidAmount, depositAmount, bidMethod, selectedBank);
        Map<String, Object> data = serviceResponse.getData();
        
        return processServiceResponse(data, model, "payment/bid-submitted");
    }

    /**
     * 결제 실패 페이지
     */
    @GetMapping("/fail")
    public String paymentFail(@RequestParam(name = "message", required = false) String message, Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handlePaymentFailPageRequest(message);
        Map<String, Object> data = serviceResponse.getData();
        model.addAllAttributes(data);
        return "payment/payment";
    }

    /**
     * 내 결제 내역 조회
     */
    @GetMapping("/my-payments")
    public String myPayments(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handleMyPaymentsPageRequest(memberId);
        Map<String, Object> data = serviceResponse.getData();
        
        return processServiceResponse(data, model, "payment/my-payments");
    }

    /**
     * 결제 상세 정보 조회
     */
    @GetMapping("/detail/{paymentId}")
    public String paymentDetail(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @PathVariable("paymentId") Long paymentId,
            Model model) {
        ServiceResponse<Map<String, Object>> serviceResponse = paymentDetailService.handlePaymentDetailPageRequest(memberId, paymentId);
        Map<String, Object> data = serviceResponse.getData();
        
        return processServiceResponse(data, model, "payment/bid-detail");
    }

    /**
     * 서비스 응답 처리 (redirect/error 체크 및 모델 설정)
     */
    private String processServiceResponse(Map<String, Object> data, Model model, String defaultView) {
        if (data.containsKey("redirect")) {
            return "redirect:" + data.get("redirect");
        }
        
        // 에러가 있어도 defaultView를 반환 (입찰서 작성 페이지는 에러 메시지를 표시할 수 있음)
        // 단, pageType이 "fail"이고 defaultView가 "payment/payment"가 아닌 경우에만 payment/payment로 이동
        if (data.containsKey("error") && !defaultView.equals("payment/bid-form") && !defaultView.equals("payment/bid-submitted")) {
            model.addAllAttributes(data);
            return "payment/payment";
        }
        
        model.addAllAttributes(data);
        return defaultView;
    }

    /**
     * 결제 취소
     */
    @PostMapping("/cancel/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelPayment(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @PathVariable("paymentId") Long paymentId,
            @RequestBody Map<String, String> requestBody) {
        String reason = requestBody != null ? requestBody.get("reason") : null;
        return paymentDetailService.handleCancelPaymentRequest(memberId, paymentId, reason).toResponseEntity();
    }

    /**
     * 입찰내역 삭제 (DELETE 메서드)
     */
    @DeleteMapping("/delete/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePayment(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @PathVariable("paymentId") Long paymentId) {
        return paymentDetailService.handleDeletePaymentRequest(memberId, paymentId).toResponseEntity();
    }

    /**
     * 입찰내역 삭제 (POST 메서드 - 대안)
     */
    @PostMapping("/delete/{paymentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePaymentPost(
            @SessionAttribute(name = "loginId", required = false) String memberId,
            @PathVariable("paymentId") Long paymentId) {
        return paymentDetailService.handleDeletePaymentRequest(memberId, paymentId).toResponseEntity();
    }
}


