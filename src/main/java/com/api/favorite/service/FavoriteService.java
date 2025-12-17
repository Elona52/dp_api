package com.api.favorite.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.api.favorite.domain.Favorite;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.member.domain.Member;
import com.api.favorite.mapper.FavoriteMapper;
import com.api.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j  
@Service 
@RequiredArgsConstructor  

@Transactional(readOnly = true) 
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final MemberMapper memberMapper;
    private final ItemRestService itemService;
    private Long extractItemId(Map<String, Object> requestBody) {
        log.info("========================================");
        log.info("=== extractItemId 메서드 호출 ===");
        log.info("========================================");
        if (requestBody == null) {
            log.error("❌ extractItemId: requestBody가 null");
            return null;
        }

        log.info("extractItemId: requestBody keys={}", requestBody.keySet());
        log.info("extractItemId: requestBody 전체={}", requestBody);
        log.info("extractItemId: itemId 값={}, 타입={}", 
            requestBody.get("itemId"), 
            requestBody.get("itemId") != null ? requestBody.get("itemId").getClass().getName() : "null");
        log.info("extractItemId: cltrNo 값={}, 타입={}", 
            requestBody.get("cltrNo"),
            requestBody.get("cltrNo") != null ? requestBody.get("cltrNo").getClass().getName() : "null");

        // itemId가 있으면 직접 사용
        Object itemIdObj = requestBody.get("itemId");
        if (itemIdObj != null) {
            try {
                Long itemId;
                if (itemIdObj instanceof Number) {
                    itemId = ((Number) itemIdObj).longValue();
                } else {
                    itemId = Long.parseLong(itemIdObj.toString());
                }
                log.info("✅ extractItemId: itemId 직접 사용={}", itemId);
                return itemId;
            } catch (NumberFormatException e) {
                log.error("❌ extractItemId: itemId 파싱 실패 - value={}, error={}", 
                    itemIdObj, e.getMessage());
            }
        }

        // cltrNo (물건번호)로 조회
        Object cltrNoObj = requestBody.get("cltrNo");
        if (cltrNoObj != null) {
            String cltrNo = cltrNoObj.toString().trim();
            log.info("extractItemId: cltrNo로 조회={}", cltrNo);
            if (!cltrNo.isEmpty() && !cltrNo.equals("null")) {
                Long itemId = getItemIdByCltrNo(cltrNo);
                log.info("extractItemId: cltrNo로 조회 결과 itemId={}", itemId);
                if (itemId == null) {
                    log.error("❌ extractItemId: cltrNo로 itemId를 찾을 수 없음 - cltrNo={}", cltrNo);
                } else {
                    log.info("✅ extractItemId: cltrNo로 itemId 찾기 성공 - cltrNo={}, itemId={}", cltrNo, itemId);
                }
                return itemId;
            } else {
                log.warn("⚠️ extractItemId: cltrNo가 빈 문자열이거나 'null' 문자열임");
            }
        }
        log.error("❌ extractItemId: itemId, itemPlnmNo, cltrNo 모두 없거나 유효하지 않음 - requestBody={}", requestBody);
        log.info("========================================");
        return null;
    }

    /**
     * 즐겨찾기 추가 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleAddFavoriteRequest(
            String userId,
            Map<String, Object> requestBody) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== 즐겨찾기 추가 요청 ===");
            log.info("userId: {}, requestBody: {}", userId, requestBody);
            
            if (userId == null || userId.isEmpty()) {
                log.warn("로그인 필요: userId가 null이거나 비어있음");
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            Long itemId = extractItemId(requestBody);
            log.info("extractItemId 결과: itemId={}", itemId);
            
            if (itemId == null) {
                log.warn("itemId 추출 실패: requestBody={}", requestBody);
                response.put("success", false);
                response.put("message", "itemId 또는 cltrNo가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            Favorite favorite = addFavorite(userId, itemId);
            log.info("즐겨찾기 추가 성공: favoriteId={}, userId={}, itemId={}", 
                favorite.getFavoriteId(), userId, itemId);
            
            response.put("success", true);
            response.put("message", "즐겨찾기에 추가되었습니다.");
            response.put("favorite", favorite);
            return ServiceResponse.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
        } catch (Exception e) {
            log.error("즐겨찾기 추가 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 추가 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 삭제 요청 처리
     */
    public ServiceResponse<Map<String, Object>> handleRemoveFavoriteRequest(String userId, Long favoriteId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("즐겨찾기 삭제 요청 처리: userId={}, favoriteId={}", userId, favoriteId);
            
            if (favoriteId == null) {
                response.put("success", false);
                response.put("message", "favoriteId가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }
            
            // 로그인 체크
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            // 즐겨찾기 정보 조회하여 권한 확인
            Favorite favorite = favoriteMapper.getFavoriteById(favoriteId);
            if (favorite == null) {
                response.put("success", false);
                response.put("message", "즐겨찾기를 찾을 수 없습니다.");
                return ServiceResponse.of(HttpStatus.NOT_FOUND, response);
            }
            
            // 자신의 즐겨찾기만 삭제 가능
            if (!userId.equals(favorite.getUserId())) {
                log.warn("권한 없음: userId={}, favorite.userId={}", userId, favorite.getUserId());
                response.put("success", false);
                response.put("message", "삭제 권한이 없습니다.");
                return ServiceResponse.of(HttpStatus.FORBIDDEN, response);
            }

            removeFavorite(favoriteId);
            response.put("success", true);
            response.put("message", "즐겨찾기가 삭제되었습니다.");
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 삭제 중 오류: userId={}, favoriteId={}", userId, favoriteId, e);
            response.put("success", false);
            response.put("message", "즐겨찾기 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 목록 응답 생성 (화면 표시용 데이터 가공 포함)
     */
    public ServiceResponse<Map<String, Object>> handleFavoritesResponse(String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", false);
                response.put("message", "로그인이 필요한 서비스입니다.");
                return ServiceResponse.of(HttpStatus.UNAUTHORIZED, response);
            }

            List<Favorite> favorites = getFavoritesByMemberId(userId);
            
            // 화면 표시용 데이터 가공
            List<Map<String, Object>> processedFavorites = processFavoritesForDisplay(favorites);
            
            response.put("success", true);
            response.put("favorites", processedFavorites);
            response.put("count", processedFavorites != null ? processedFavorites.size() : 0);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 목록 조회 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }
    
    /**
     * 즐겨찾기 목록을 화면 표시용 데이터로 가공
     * - 날짜 포맷팅
     * - 가격 포맷팅
     * - 가격 비율 계산
     * - ItemDetail 정보 포함
     */
    private List<Map<String, Object>> processFavoritesForDisplay(List<Favorite> favorites) {
        List<Map<String, Object>> processedList = new ArrayList<>();
        
        if (favorites == null || favorites.isEmpty()) {
            return processedList;
        }
        
        DecimalFormat priceFormatter = new DecimalFormat("#,###");
        
        for (Favorite favorite : favorites) {
            Map<String, Object> favoriteMap = new HashMap<>();
            
            // 기본 즐겨찾기 정보
            favoriteMap.put("favoriteId", favorite.getFavoriteId());
            favoriteMap.put("id", favorite.getFavoriteId()); // 호환성
            favoriteMap.put("itemId", favorite.getItemId());
            favoriteMap.put("userId", favorite.getUserId());
            favoriteMap.put("createdAt", favorite.getCreatedAt());
            
            // ItemDetail 조회
            ItemDetail itemDetail = null;
            if (favorite.getItemId() != null) {
                try {
                    itemDetail = itemService.getItemDetail(favorite.getItemId());
                } catch (Exception e) {
                    log.warn("즐겨찾기 항목의 물건 정보 조회 실패: itemId={}, error={}", 
                        favorite.getItemId(), e.getMessage());
                }
            }
            
            // ItemDetail 정보 추가
            if (itemDetail != null) {
                favoriteMap.put("item", itemDetail);
                
                // 비즈니스 로직: 날짜 포맷팅 (bidEnd를 사용, 없으면 bidStart)
                String formattedDate = "-";
                if (itemDetail.getBidEnd() != null) {
                    formattedDate = formatDate(itemDetail.getBidEnd());
                } else if (itemDetail.getBidStart() != null) {
                    formattedDate = formatDate(itemDetail.getBidStart());
                }
                favoriteMap.put("formattedDate", formattedDate);
                
                // 비즈니스 로직: 가격 포맷팅
                Long minPrice = itemDetail.getMinBidPriceMin() != null ? itemDetail.getMinBidPriceMin() : 
                               (itemDetail.getMinBidPriceMax() != null ? itemDetail.getMinBidPriceMax() : 0L);
                Long appraisalPrice = itemDetail.getAppraisalAmountMin() != null ? itemDetail.getAppraisalAmountMin() : 
                                     (itemDetail.getAppraisalAmountMax() != null ? itemDetail.getAppraisalAmountMax() : 0L);
                
                favoriteMap.put("minPrice", minPrice);
                favoriteMap.put("minPriceFormatted", minPrice > 0 ? priceFormatter.format(minPrice) : "-");
                favoriteMap.put("appraisalPrice", appraisalPrice);
                favoriteMap.put("appraisalPriceFormatted", appraisalPrice > 0 ? priceFormatter.format(appraisalPrice) : "-");
                
                // 비즈니스 로직: 가격 비율 계산
                String pricePercent = "";
                if (appraisalPrice > 0 && minPrice > 0) {
                    int percent = (int) Math.round((minPrice * 100.0) / appraisalPrice);
                    pricePercent = "(" + percent + "%)";
                }
                favoriteMap.put("pricePercent", pricePercent);
                
                // 주소 정보
                String address = itemDetail.getNmrAddress() != null ? itemDetail.getNmrAddress() : 
                                (itemDetail.getRoadName() != null ? itemDetail.getRoadName() : 
                                (itemDetail.getAddress() != null ? itemDetail.getAddress() : ""));
                favoriteMap.put("address", address);
                
                // 물건명
                String itemName = itemDetail.getAddress() != null ? itemDetail.getAddress() : "";
                favoriteMap.put("itemName", itemName);
                
                // 관리번호
                String cltrNo = itemDetail.getCltrMnmtNo() != null ? itemDetail.getCltrMnmtNo() : "";
                favoriteMap.put("cltrNo", cltrNo);
                
                // 유찰 횟수 (bidCount 사용)
                int uscbCnt = itemDetail.getBidCount() != null ? itemDetail.getBidCount() : 0;
                favoriteMap.put("uscbCnt", uscbCnt);
                
                // 상품명 (goodsDetail 사용)
                String goodsNm = itemDetail.getGoodsDetail() != null ? itemDetail.getGoodsDetail() : "";
                favoriteMap.put("goodsNm", goodsNm);
            } else {
                // ItemDetail이 없는 경우 기본값
                favoriteMap.put("item", null);
                favoriteMap.put("formattedDate", "-");
                favoriteMap.put("minPrice", 0L);
                favoriteMap.put("minPriceFormatted", "-");
                favoriteMap.put("appraisalPrice", 0L);
                favoriteMap.put("appraisalPriceFormatted", "-");
                favoriteMap.put("pricePercent", "");
                favoriteMap.put("address", "");
                favoriteMap.put("itemName", "물건 정보 없음");
                favoriteMap.put("cltrNo", "");
                favoriteMap.put("uscbCnt", 0);
                favoriteMap.put("goodsNm", "");
            }
            
            processedList.add(favoriteMap);
        }
        
        return processedList;
    }
    
    /**
     * 날짜 포맷팅 (LocalDateTime -> YYYY.MM.DD)
     */
    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return String.format("%04d.%02d.%02d", 
            dateTime.getYear(), 
            dateTime.getMonthValue(), 
            dateTime.getDayOfMonth());
    }

    /**
     * 즐겨찾기 여부 확인 응답 생성
     */
    public ServiceResponse<Map<String, Object>> handleFavoriteCheck(String userId, Long itemId, String cltrNo, String itemPlnmNo) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.isEmpty()) {
                response.put("success", true);
                response.put("isFavorite", false);
                return ServiceResponse.ok(response);
            }

            boolean isFavorite = false;
            if (itemId != null) {
                isFavorite = isFavorite(userId, itemId);
            } else if (itemPlnmNo != null && !itemPlnmNo.isEmpty()) {
                isFavorite = isFavoriteByCltrNo(userId, itemPlnmNo);
            } else if (cltrNo != null && !cltrNo.isEmpty()) {
                isFavorite = isFavoriteByCltrNo(userId, cltrNo);
            } else {
                response.put("success", false);
                response.put("message", "itemId, itemPlnmNo 또는 cltrNo가 필요합니다.");
                return ServiceResponse.of(HttpStatus.BAD_REQUEST, response);
            }

            response.put("success", true);
            response.put("isFavorite", isFavorite);
            return ServiceResponse.ok(response);

        } catch (Exception e) {
            log.error("즐겨찾기 확인 중 오류", e);
            response.put("success", false);
            response.put("message", "즐겨찾기 확인 중 오류가 발생했습니다: " + e.getMessage());
            return ServiceResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
    }

    /**
     * 즐겨찾기 추가 (새 구조: itemId 사용)
     */
    @Transactional
    public Favorite addFavorite(String userId, Long itemId) {
        log.info("=== addFavorite 메서드 호출 ===");
        log.info("입력 파라미터: userId={}, itemId={}", userId, itemId);
        
        // 입력값 검증
        if (userId == null || userId.trim().isEmpty()) {
            log.error("회원 ID가 null이거나 비어있음");
            throw new IllegalArgumentException("회원 ID가 필요합니다.");
        }
        if (itemId == null) {
            log.error("물건 ID가 null");
            throw new IllegalArgumentException("물건 ID가 필요합니다.");
        }
        
        log.info("입력값 검증 통과");
        
        // 이미 즐겨찾기에 있는지 확인
        log.info("기존 즐겨찾기 확인 중: userId={}, itemId={}", userId, itemId);
        Favorite existing = favoriteMapper.getFavoriteByMemberAndItem(userId, itemId);
        if (existing != null) {
            log.info("이미 즐겨찾기에 존재: favoriteId={}, userId={}, itemId={}", 
                existing.getFavoriteId(), userId, itemId);
            return existing; // 이미 존재하면 기존 객체 반환
        }
        log.info("기존 즐겨찾기 없음 - 새로 추가 진행");
        
        // 물건 정보 조회 (itemId는 plnmNo를 의미)
        log.info("물건 정보 조회 중: itemId(plnmNo)={}", itemId);
        ItemDetail item = itemService.getItemDetail(itemId);
        if (item == null) {
            log.warn("물건 정보를 찾을 수 없음: itemId(plnmNo)={}", itemId);
        } else {
            log.info("물건 정보 조회 성공: plnmNo={}, address={}", item.getPlnmNo(), item.getAddress());
        }
        
        // 새로운 즐겨찾기 추가
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        log.info("Favorite 객체 생성: userId={}, itemId={}", userId, itemId);
        
        try {
            log.info("데이터베이스에 즐겨찾기 INSERT 시도");
            log.info("INSERT할 데이터: userId={}, itemId={}", userId, itemId);
            
            // 외래키 제약조건 확인을 위한 사전 검증
            Member member = memberMapper.getMemberInfo(userId);
            if (member == null) {
                log.error("회원을 찾을 수 없음: userId={}", userId);
                throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다: " + userId);
            }
            log.info("회원 확인 성공: userId={}, name={}", userId, member.getName());
            
            // item_basic 확인 (itemId는 plnmNo)
            if (item == null) {
                log.error("물건 정보를 찾을 수 없음: itemId(plnmNo)={}", itemId);
                throw new IllegalArgumentException("물건 정보를 찾을 수 없습니다: itemId(plnmNo)=" + itemId);
            }
            log.info("물건 확인 성공: itemId(plnmNo)={}, address={}", itemId, item.getAddress());
            
            favoriteMapper.insertFavorite(favorite);
            log.info("즐겨찾기 INSERT 실행 완료: favoriteId={}, userId={}, itemId={}", 
                favorite.getFavoriteId(), userId, itemId);
            
            // INSERT 후 즉시 조회하여 확인
            Favorite insertedFavorite = favoriteMapper.getFavoriteByMemberAndItem(userId, itemId);
            if (insertedFavorite != null) {
                log.info("✅ 즐겨찾기 추가 성공 확인: favoriteId={}, userId={}, itemId={}", 
                    insertedFavorite.getFavoriteId(), userId, itemId);
            } else {
                log.error("❌ 즐겨찾기 추가 후 조회 실패: userId={}, itemId={}", userId, itemId);
                throw new RuntimeException("즐겨찾기 추가 후 조회에 실패했습니다. 데이터베이스에 저장되지 않았을 수 있습니다.");
            }
            
            // 관심수 증가 (view_count 업데이트는 필요시 ItemService에 메서드 추가)
            // 현재는 item_basic 테이블에 view_count가 없으므로 주석 처리
            // if (item != null && item.getPlnmNo() != null) {
            //     log.info("관심수 증가 시도: plnmNo={}", item.getPlnmNo());
            //     // itemService.incrementViewCount(item.getPlnmNo());
            //     log.info("관심수 증가 완료: plnmNo={}", item.getPlnmNo());
            // }
        } catch (Exception e) {
            log.error("즐겨찾기 추가 실패: userId={}, itemId={}, error={}", 
                userId, itemId, e.getMessage(), e);
            log.error("예외 스택 트레이스:", e);
            throw new RuntimeException("즐겨찾기 추가에 실패했습니다: " + e.getMessage(), e);
        }
        
        return favorite;
    }

    /**
     * 즐겨찾기 삭제
     * @param favoriteId 즐겨찾기 ID
     */
    @Transactional
    public void removeFavorite(Long favoriteId) {
        try {
            favoriteMapper.getFavoriteById(favoriteId);
            
            favoriteMapper.deleteFavorite(favoriteId);
            log.info("즐겨찾기 삭제 성공: favoriteId={}", favoriteId);
            
            // 관심수 감소는 필요시 ItemService에 메서드 추가
            // if (favorite != null && favorite.getItemId() != null) {
            //     log.debug("관심수 감소: plnmNo={}", favorite.getItemId());
            //     // itemService.decrementViewCount(favorite.getItemId());
            // }
        } catch (Exception e) {
            log.error("즐겨찾기 삭제 실패: favoriteId={}, error={}", favoriteId, e.getMessage(), e);
            throw new RuntimeException("즐겨찾기 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 특정 회원의 즐겨찾기 목록 조회 (물건 정보 포함)
     * @param memberId 회원 ID
     * @return 즐겨찾기 목록
     */
    public List<Favorite> getFavoritesByMemberId(String memberId) {
        log.info("즐겨찾기 목록 조회 시작: memberId={}", memberId);
        
        if (memberId == null || memberId.trim().isEmpty()) {
            log.warn("⚠️ memberId가 null이거나 비어있습니다.");
            return new java.util.ArrayList<>();
        }
        
        try {
            List<Favorite> favorites = favoriteMapper.getFavoritesByMemberId(memberId);
            log.info("✅ 즐겨찾기 목록 조회 완료: memberId={}, count={}", memberId, favorites != null ? favorites.size() : 0);
            
            if (favorites != null && !favorites.isEmpty()) {
                log.info("📋 즐겨찾기 상세 정보:");
                for (int i = 0; i < favorites.size(); i++) {
                    Favorite fav = favorites.get(i);
                    // itemId(plnmNo)로 ItemDetail 조회
                    ItemDetail itemDetail = fav.getItemId() != null ? itemService.getItemDetail(fav.getItemId()) : null;
                    String itemName = itemDetail != null ? itemDetail.getAddress() : "null";
                    log.info("  [{}] favoriteId={}, itemId(plnmNo)={}, address={}", 
                        i + 1, fav.getFavoriteId(), fav.getItemId(), itemName);
                }
            } else {
                log.warn("⚠️ 즐겨찾기 목록이 비어있습니다.");
                log.warn("💡 데이터베이스 확인 쿼리: SELECT * FROM favorite WHERE member_id = '{}'", memberId);
            }
            
            return favorites != null ? favorites : new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("❌ 즐겨찾기 목록 조회 중 오류: memberId={}, error={}", memberId, e.getMessage(), e);
            throw new RuntimeException("즐겨찾기 목록 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 회원의 특정 물건 즐겨찾기 여부 확인
     * @param memberId 회원 ID
     * @param itemId 물건 ID
     * @return 즐겨찾기 여부
     */
    public boolean isFavorite(String memberId, Long itemId) {
        Favorite favorite = favoriteMapper.getFavoriteByMemberAndItem(memberId, itemId);
        return favorite != null;
    }
    
    /**
     * cltrNo (물건번호)로 즐겨찾기 여부 확인 (기존 호환성 유지)
     * @param memberId 회원 ID
     * @param cltrNo 물건번호
     * @return 즐겨찾기 여부
     */
    public boolean isFavoriteByCltrNo(String memberId, String cltrNo) {
        try {
            // cltrNo는 item_detail.cltr_mnmt_no를 의미할 수 있음
            // 하지만 현재 구조에서는 plnmNo로 직접 조회하는 것이 더 정확
            // cltrNo로 plnmNo를 찾는 로직이 필요하면 ItemMapper에 추가 필요
            log.warn("cltrNo로 즐겨찾기 확인은 현재 지원하지 않습니다. plnmNo를 사용해주세요.");
        } catch (Exception e) {
            log.warn("cltrNo로 즐겨찾기 확인 중 오류: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * cltrNo (물건번호)로 itemId(plnmNo) 조회 (컨트롤러에서 사용)
     * @param cltrNo 물건번호 (cltr_mnmt_no)
     * @return itemId(plnmNo) (없으면 null)
     */
    public Long getItemIdByCltrNo(String cltrNo) {
        try {
            log.info("getItemIdByCltrNo: cltrNo={}", cltrNo);
            
            if (cltrNo == null || cltrNo.trim().isEmpty()) {
                log.warn("getItemIdByCltrNo: cltrNo가 null이거나 비어있음");
                return null;
            }
            
            // cltrNo(cltr_mnmt_no)로 ItemDetail 조회
            ItemDetail itemDetail = itemService.getItemDetailByCltrMnmtNo(cltrNo.trim());
            
            if (itemDetail == null || itemDetail.getPlnmNo() == null) {
                log.warn("getItemIdByCltrNo: cltrNo로 물건을 찾을 수 없음 - cltrNo={}", cltrNo);
                return null;
            }
            
            Long plnmNo = itemDetail.getPlnmNo();
            log.info("✅ getItemIdByCltrNo: cltrNo로 plnmNo 찾기 성공 - cltrNo={}, plnmNo={}", cltrNo, plnmNo);
            return plnmNo;
            
        } catch (Exception e) {
            log.error("getItemIdByCltrNo: cltrNo로 itemId 조회 중 오류 - cltrNo={}, error={}", 
                cltrNo, e.getMessage(), e);
            return null;
        }
    }
}
