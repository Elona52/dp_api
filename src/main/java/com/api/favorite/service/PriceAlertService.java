package com.api.favorite.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.favorite.domain.Favorite;
import com.api.favorite.domain.PriceAlert;
import com.api.favorite.mapper.FavoriteMapper;
import com.api.item.domain.Item;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.member.domain.Member;
import com.api.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceAlertService {

    private final FavoriteMapper favoriteMapper;
    private final ItemRestService itemService;
    private final MemberMapper memberMapper;
    
    @Autowired(required = false)
    private JavaMailSender mailSender; 

    /**
     * 가격 알림 히스토리 응답 생성
     */
    public ServiceResponse<Map<String, Object>> handlePriceAlertsResponse(String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            List<PriceAlert> alerts = getPriceAlertsByMemberId(userId);
            response.put("success", true);
            response.put("alerts", alerts);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("가격 알림 히스토리 조회 중 오류", e);
            response.put("success", false);
            response.put("message", "알림 히스토리 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 특정 회원의 가격 알림 히스토리 조회
     * @param memberId 회원 ID
     * @return 가격 알림 목록
     */
    public List<PriceAlert> getPriceAlertsByMemberId(String memberId) {
        return favoriteMapper.getPriceAlertsByMemberId(memberId);
    }

    /**
     * 가격 하락 알림 이메일 전송
     * 
     * @param toEmail 받는 사람 이메일
     * @param memberName 회원 이름
     * @param favorite 즐겨찾기 정보
     * @param newPrice 새로운 가격
     * @param currentPrice 현재 가격
     */
    @Async // 비동기 처리
    public void sendPriceDropAlert(String toEmail, String memberName, Favorite favorite, Long newPrice, Long currentPrice) {
        try {
            // itemId(plnmNo)로 ItemDetail 조회
            ItemDetail itemDetail = favorite.getItemId() != null ? itemService.getItemDetail(favorite.getItemId()) : null;
            String itemName = itemDetail != null && itemDetail.getAddress() != null 
                ? itemDetail.getAddress() : "상품";
            String cltrNo = itemDetail != null && itemDetail.getCltrMnmtNo() != null 
                ? itemDetail.getCltrMnmtNo() : "";
            
            String subject = "[가격 하락 알림] " + itemName;
            StringBuilder content = new StringBuilder();
            content.append("안녕하세요, ").append(memberName).append("님!\n\n");
            content.append("즐겨찾기하신 상품의 가격이 하락했습니다.\n\n");
            content.append("===========================================\n");
            content.append("상품명: ").append(itemName).append("\n");
            content.append("공고번호: ").append(cltrNo).append("\n");
            content.append("이전 가격: ").append(formatPrice(currentPrice)).append("원\n");
            content.append("현재 가격: ").append(formatPrice(newPrice)).append("원\n");

            if (currentPrice != null && currentPrice > 0) {
                long priceDrop = currentPrice - newPrice;
                double dropRate = (double) priceDrop / currentPrice * 100;
                content.append("하락 금액: ").append(formatPrice(priceDrop)).append("원 (")
                       .append(String.format("%.1f", dropRate)).append("%)\n");
            }

            content.append("===========================================\n\n");
            content.append("자세한 내용은 사이트에서 확인해주세요.\n\n");
            content.append("감사합니다.");

            sendEmailWithRetry(toEmail, subject, content.toString(), 3);

            log.info("가격 하락 알림 이메일 전송 성공: {} -> {}", toEmail, itemName);

        } catch (Exception e) {
            log.error("가격 하락 알림 이메일 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 일반 알림 이메일 전송
     * 
     * @param toEmail 받는 사람 이메일
     * @param subject 제목
     * @param content 내용
     */
    public void sendEmail(String toEmail, String subject, String content) {
        sendEmailWithRetry(toEmail, subject, content, 3);
    }

    /**
     * 이메일 재시도 처리
     */
    private void sendEmailWithRetry(String toEmail, String subject, String content, int retryCount) {
        if (mailSender == null) {
            log.warn("JavaMailSender가 설정되지 않아 이메일을 전송할 수 없습니다. application.properties에 메일 설정을 추가하세요.");
            return;
        }

        for (int i = 0; i < retryCount; i++) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);

                mailSender.send(message);
                log.info("이메일 전송 성공: {}", toEmail);
                return;

            } catch (Exception e) {
                log.error("이메일 전송 실패 ({}회차): {} - {}", i + 1, toEmail, e.getMessage());
            }
        }
        throw new RuntimeException("이메일 전송에 실패했습니다. 최대 재시도 횟수 초과.");
    }

    /**
     * 가격 포맷팅
     */
    private String formatPrice(Long price) {
        if (price == null) return "0";
        return String.format("%,d", price);
    }

    /**
     * 가격 모니터링 스케줄러
     * 매일 오전 9시와 오후 6시에 실행
     */
    @Scheduled(cron = "0 0 9,18 * * *")
    @Transactional
    public void monitorPrices() {
        log.info("======================================");
        log.info("가격 모니터링 시작");
        log.info("======================================");
        
        try {
            // 알림이 활성화된 모든 즐겨찾기 조회
            List<Favorite> favorites = favoriteMapper.getActiveAlertFavorites();
            log.info("모니터링 대상 즐겨찾기 수: {}", favorites.size());
            
            if (favorites.isEmpty()) {
                log.info("모니터링할 즐겨찾기가 없습니다.");
                return;
            }
            
            // API에서 최신 상품 정보 조회
            // TODO: onbidApiService 주입 필요
            // List<Item> items = onbidApiService.getUnifyUsageCltr(null, 1, 100);
            List<Item> items = List.of(); // 임시로 빈 리스트
            log.info("API에서 조회한 상품 수: {}", items.size());
            
            int alertCount = 0;
            
            // 각 즐겨찾기에 대해 가격 확인
            for (Favorite favorite : favorites) {
                try {
                    // itemId(plnmNo)로 ItemDetail 조회
                    if (favorite.getItemId() == null) {
                        log.debug("물건 ID가 없습니다: favoriteId={}", favorite.getFavoriteId());
                        continue;
                    }
                    
                    ItemDetail itemDetail = itemService.getItemDetail(favorite.getItemId());
                    if (itemDetail == null) {
                        log.debug("물건 정보를 찾을 수 없습니다: plnmNo={}", favorite.getItemId());
                        continue;
                    }
                    
                    // 해당 상품 찾기 (API에서 조회한 items와 비교)
                    Item matchedItem = findItemByPlnmNo(items, String.valueOf(favorite.getItemId()));
                    
                    if (matchedItem == null) {
                        log.debug("상품을 찾을 수 없습니다: plnmNo={}", favorite.getItemId());
                        continue;
                    }
                    
                    // 현재 가격 (최저입찰가 사용)
                    Long newPrice = matchedItem.getMinBidPriceMin();
                    if (newPrice == null) {
                        log.debug("가격 정보가 없습니다: plnmNo={}", favorite.getItemId());
                        continue;
                    }
                    
                    // 기존 가격 (물건의 최저입찰가 사용)
                    Long currentPrice = itemDetail.getMinBidPriceMin();
                    
                    // 가격 변동 확인
                    if (currentPrice != null && newPrice < currentPrice) {
                        // 최근 알림 기록 확인
                        PriceAlert lastAlert = favoriteMapper.getLastPriceAlertByFavoriteId(favorite.getFavoriteId());
                        if (lastAlert != null && lastAlert.getNewPrice() != null && lastAlert.getNewPrice().equals(newPrice)) {
                            log.info("이미 같은 가격으로 알림 전송됨: {} -> {}", itemDetail.getAddress(), newPrice);
                            continue; // 중복 알림 방지
                        }

                        // 알림 전송
                        Member member = memberMapper.getMemberInfo(favorite.getUserId());

                        if (member != null && member.getMail() != null && !member.getMail().isEmpty()) {
                            sendPriceDropAlert(
                                member.getMail(),
                                member.getName(),
                                favorite,
                                newPrice,
                                currentPrice
                            );

                            // 알림 히스토리 저장
                            PriceAlert alert = new PriceAlert();
                            alert.setFavoriteId(favorite.getFavoriteId());
                            alert.setMemberId(favorite.getUserId());
                            alert.setItemPlnmNo(favorite.getItemId()); // 물건번호 직접 저장 (favorite 삭제되어도 조회 가능)
                            alert.setPreviousPrice(currentPrice);
                            alert.setNewPrice(newPrice);
                            alert.setAlertSent(true);
                            alert.setSentDate(new Timestamp(System.currentTimeMillis()));

                            favoriteMapper.insertPriceAlert(alert);

                            alertCount++;
                            log.info("알림 전송 완료: {} -> {}", member.getMail(), itemDetail.getAddress());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("즐겨찾기 처리 중 오류: favoriteId={} - {}", 
                        favorite.getFavoriteId(), e.getMessage(), e);
                }
            }
            
            log.info("======================================");
            log.info("가격 모니터링 완료 - 전송된 알림 수: {}", alertCount);
            log.info("======================================");
            
        } catch (Exception e) {
            log.error("가격 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 수동으로 가격 모니터링 실행 (테스트용)
     */
    public void monitorPricesManually() {
        log.info("수동 가격 모니터링 실행");
        monitorPrices();
    }

    /**
     * plnmNo로 Item 찾기
     * @param items Item 리스트
     * @param plnmNo 물건번호
     * @return 매칭되는 Item 또는 null
     */
    private Item findItemByPlnmNo(List<Item> items, String plnmNo) {
        if (items == null || plnmNo == null) {
            return null;
        }
        
        try {
            Long plnmNoLong = Long.parseLong(plnmNo);
            return items.stream()
                .filter(item -> item.getPlnmNo() != null && item.getPlnmNo().equals(plnmNoLong))
                .findFirst()
                .orElse(null);
        } catch (NumberFormatException e) {
            log.warn("plnmNo 파싱 실패: {}", plnmNo);
            return null;
        }
    }
}

