package com.api.item.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.api.item.dto.ItemDetail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Item View 전용 서비스
 * 메인 페이지 등 View에서 필요한 데이터를 가공하여 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRestService itemService;

    /**
     * 메인 페이지용 신규물건 공지 데이터 조회
     * @param limit 조회할 개수
     * @return 공지사항 리스트
     */
    public List<NoticeItem> getMainPageNotices(int limit) {
        // API에서 직접 신규물건 조회
        List<ItemDetail> newItems = itemService.fetchNewItemsFromApi(1, "서울특별시");
        if (newItems.size() > limit) {
            newItems = newItems.subList(0, limit);
        }
        return newItems.stream()
            .map(item -> {
                NoticeItem notice = new NoticeItem();
                notice.setCltrNo(item.getCltrMnmtNo() != null ? item.getCltrMnmtNo() : "");
                notice.setTitle(item.getAddress() != null ? item.getAddress() : "신규물건 등록");
                notice.setDate(item.getBidStart() != null ? item.getBidStart() : LocalDateTime.now());
                return notice;
            })
            .collect(Collectors.toList());
    }

    /**
     * 메인 페이지용 용도별 물건정보 통계 조회
     * API에서 가져온 물건들을 카테고리로 묶어서 반환
     * 개수 기준 내림차순 정렬
     * @return 카테고리별 통계 Map (카테고리명 -> 개수)
     */
    public Map<String, Integer> getMainPageCategoryStats() {
        // API에서 용도별 통합 조회 (최대 3페이지 = 600건)
        Map<String, Integer> categoryStats = new LinkedHashMap<>();
        
        try {
            List<ItemDetail> allItems = new java.util.ArrayList<>();
            for (int page = 1; page <= 3; page++) {
                List<ItemDetail> items = itemService.fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                allItems.addAll(items);
            }
            
            log.info("📊 API에서 조회된 물건 수: {}개", allItems.size());
            
            // 카테고리별로 그룹화
            Map<String, Long> categoryCounts = allItems.stream()
                .filter(item -> item.getAssetCategory() != null && !item.getAssetCategory().trim().isEmpty())
                .filter(item -> !item.getAssetCategory().contains("기타") && !item.getAssetCategory().contains("미분류"))
                .collect(Collectors.groupingBy(
                    ItemDetail::getAssetCategory,
                    Collectors.counting()
                ));
            
            // 개수 기준 내림차순 정렬
            categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(12) // 최대 12개만
                .forEach(entry -> {
                    categoryStats.put(entry.getKey(), entry.getValue().intValue());
                    log.info("📊 카테고리: {} = {}개", entry.getKey(), entry.getValue());
                });
            
            log.info("📊 변환된 카테고리 통계: {}개 (API 데이터 기반)", categoryStats.size());
        } catch (Exception e) {
            log.error("❌ 카테고리 통계 조회 실패", e);
        }
        
        return categoryStats;
    }

    /**
     * 메인 페이지용 금주의 경매일정 데이터 조회
     * 오늘 마감하는 물건들을 조회하여 반환
     * @param limit 조회할 개수
     * @return 경매일정 리스트
     */
    public List<ScheduleItem> getMainPageScheduleList(int limit) {
        // API에서 용도별 통합 조회 후 오늘 마감하는 물건 필터링
        List<ItemDetail> todayItems = new java.util.ArrayList<>();
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime todayStart = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = today.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        
        try {
            // 최대 3페이지 조회하여 오늘 마감하는 물건 찾기
            for (int page = 1; page <= 3; page++) {
                List<ItemDetail> items = itemService.fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                for (ItemDetail item : items) {
                    if (item.getBidEnd() != null && 
                        !item.getBidEnd().isBefore(todayStart) && 
                        !item.getBidEnd().isAfter(todayEnd)) {
                        todayItems.add(item);
                        if (todayItems.size() >= limit) break;
                    }
                }
                if (todayItems.size() >= limit) break;
            }
        } catch (Exception e) {
            log.error("❌ 오늘 마감하는 물건 조회 실패", e);
        }
        
        return todayItems.stream()
            .map(item -> {
                ScheduleItem schedule = new ScheduleItem();
                schedule.setCltrNo(item.getCltrMnmtNo() != null ? item.getCltrMnmtNo() : "");
                // API에서 가져온 주소 그대로 사용 (nmrAddress 우선, 없으면 address 사용)
                String region = item.getNmrAddress() != null && !item.getNmrAddress().isEmpty() 
                    ? item.getNmrAddress() 
                    : (item.getAddress() != null ? item.getAddress() : "지역정보없음");
                schedule.setRegion(region);
                schedule.setPbctClsDtm(item.getBidEnd() != null ? 
                    item.getBidEnd().format(DateTimeFormatter.ofPattern("yyyyMMdd")) : "");
                return schedule;
            })
            .collect(Collectors.toList());
    }

    /**
     * 메인 페이지용 50% 체감 물건 목록 조회
     * @param limit 조회할 개수
     * @return 할인 물건 리스트
     */
    public List<DiscountItem> getMainPageDiscountList(int limit) {
        // API에서 직접 50% 체감 물건 조회
        List<ItemDetail> discountItems = itemService.fetchDiscountItemsFromApi(1, "서울특별시");
        if (discountItems.size() > limit) {
            discountItems = discountItems.subList(0, limit);
        }
        return discountItems.stream()
            .map(item -> {
                DiscountItem discount = new DiscountItem();
                discount.setCltrNo(item.getCltrMnmtNo() != null ? item.getCltrMnmtNo() : "");
                discount.setName(item.getAddress() != null ? item.getAddress() : "");
                discount.setStartPrice(item.getMinBidPriceMin() != null ? item.getMinBidPriceMin() : 0L);
                discount.setEndPrice(item.getAppraisalAmountMax() != null ? item.getAppraisalAmountMax() : 0L);
                discount.setApiItem(item.getCltrMnmtNo() != null && !item.getCltrMnmtNo().isEmpty());
                discount.setNo(item.getPlnmNo());
                return discount;
            })
            .collect(Collectors.toList());
    }

    /**
     * API 상세 페이지용 데이터 준비
     * @param itemId plnmNo (공고번호) - API에서는 사용하지 않으므로 cltrNo로 조회 필요
     * @return ApiDetailData 객체 (itemHistory, normalizedAddress, isRealEstate 포함)
     */
    public ApiDetailData getApiDetailData(Long itemId) {
        // itemId로는 API에서 직접 조회할 수 없으므로 빈 데이터 반환
        // cltrNo를 사용해야 함
        log.warn("⚠️ itemId로는 API에서 직접 조회할 수 없습니다. cltrNo를 사용해주세요.");
        return new ApiDetailData();
    }

    /**
     * API 상세 페이지용 데이터 준비 (cltrNo로 조회)
     * @param cltrNo 물건관리번호
     * @return ApiDetailData 객체
     */
    public ApiDetailData getApiDetailDataByCltrNo(String cltrNo) {
        if (cltrNo == null || cltrNo.trim().isEmpty()) {
            return new ApiDetailData();
        }
        
        // API에서 여러 페이지를 조회하여 cltrNo로 매칭
        ItemDetail itemDetail = null;
        try {
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = itemService.fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                itemDetail = items.stream()
                    .filter(item -> cltrNo.equals(item.getCltrMnmtNo()))
                    .findFirst()
                    .orElse(null);
                
                if (itemDetail != null) break;
            }
            
            // 신물건에서도 찾기
            if (itemDetail == null) {
                for (int page = 1; page <= 10; page++) {
                    List<ItemDetail> items = itemService.fetchNewItemsFromApi(page, "서울특별시");
                    if (items.isEmpty()) break;
                    
                    itemDetail = items.stream()
                        .filter(item -> cltrNo.equals(item.getCltrMnmtNo()))
                        .findFirst()
                        .orElse(null);
                    
                    if (itemDetail != null) break;
                }
            }
            
            // 감가 50% 이상에서도 찾기
            if (itemDetail == null) {
                for (int page = 1; page <= 10; page++) {
                    List<ItemDetail> items = itemService.fetchDiscountItemsFromApi(page, "서울특별시");
                    if (items.isEmpty()) break;
                    
                    itemDetail = items.stream()
                        .filter(item -> cltrNo.equals(item.getCltrMnmtNo()))
                        .findFirst()
                        .orElse(null);
                    
                    if (itemDetail != null) break;
                }
            }
        } catch (Exception e) {
            log.error("❌ API에서 물건 조회 실패: cltrNo={}", cltrNo, e);
        }
        
        if (itemDetail == null) {
            return new ApiDetailData();
        }
        
        // 날짜 검증 및 수정
        validateAndFixDates(itemDetail);
        
        ApiDetailData data = new ApiDetailData();
        data.setItemHistory(itemDetail);
        data.setNormalizedAddress(normalizeAddress(itemDetail));
        data.setIsRealEstate(isRealEstate(itemDetail));
        
        return data;
    }
    
    /**
     * 날짜 검증 및 수정 (2020년 ~ 현재 연도까지만 허용)
     */
    private void validateAndFixDates(ItemDetail itemDetail) {
        int currentYear = LocalDateTime.now().getYear();
        
        // bidEnd 검증
        if (itemDetail.getBidEnd() != null) {
            int year = itemDetail.getBidEnd().getYear();
            if (year < 2020 || year > currentYear) {
                log.debug("⚠️ 유효하지 않은 bidEnd 필터링: plnmNo={}, bidEnd={}, year={}", 
                    itemDetail.getPlnmNo(), itemDetail.getBidEnd(), year);
                itemDetail.setBidEnd(null);
            }
        }
        
        // bidStart 검증
        if (itemDetail.getBidStart() != null) {
            int year = itemDetail.getBidStart().getYear();
            if (year < 2020 || year > currentYear) {
                log.debug("⚠️ 유효하지 않은 bidStart 필터링: plnmNo={}, bidStart={}, year={}", 
                    itemDetail.getPlnmNo(), itemDetail.getBidStart(), year);
                itemDetail.setBidStart(null);
            }
        }
    }

    /**
     * 주소 정규화 (지도 표시용)
     * 우선순위: 도로명주소 > 지번주소 > 기본주소
     */
    private String normalizeAddress(ItemDetail itemDetail) {
        if (itemDetail == null) {
            return "";
        }
        
        // 1순위: 도로명주소 (roadName 또는 nmrAddress가 도로명 형식인 경우)
        if (itemDetail.getRoadName() != null && !itemDetail.getRoadName().trim().isEmpty()) {
            return itemDetail.getRoadName().trim();
        }
        
        // 2순위: 지번주소
        if (itemDetail.getNmrAddress() != null && !itemDetail.getNmrAddress().trim().isEmpty()) {
            return itemDetail.getNmrAddress().trim();
        }
        
        // 3순위: 기본 주소
        if (itemDetail.getAddress() != null && !itemDetail.getAddress().trim().isEmpty()) {
            return itemDetail.getAddress().trim();
        }
        
        return "";
    }

    /**
     * 부동산 여부 판단
     * assetCategory 또는 goodsDetail에서 부동산 관련 키워드 확인
     */
    private Boolean isRealEstate(ItemDetail itemDetail) {
        if (itemDetail == null) {
            return false;
        }
        
        String assetCategory = itemDetail.getAssetCategory();
        String goodsDetail = itemDetail.getGoodsDetail();
        
        // assetCategory에서 부동산 확인
        if (assetCategory != null) {
            String lowerCategory = assetCategory.toLowerCase();
            if (lowerCategory.contains("부동산") || 
                lowerCategory.contains("토지") || 
                lowerCategory.contains("건물") ||
                lowerCategory.contains("주거") ||
                lowerCategory.contains("상가") ||
                lowerCategory.contains("산업용")) {
                return true;
            }
        }
        
        // goodsDetail에서 부동산 관련 키워드 확인
        if (goodsDetail != null) {
            String lowerGoods = goodsDetail.toLowerCase();
            if (lowerGoods.contains("토지") || 
                lowerGoods.contains("건물") ||
                lowerGoods.contains("전") ||
                lowerGoods.contains("대지") ||
                lowerGoods.contains("㎡") ||
                lowerGoods.contains("평")) {
                return true;
            }
        }
        
        // 주소가 있는 경우도 부동산일 가능성이 높음
        if (itemDetail.getNmrAddress() != null || itemDetail.getRoadName() != null || itemDetail.getAddress() != null) {
            return true;
        }
        
        return false;
    }

    // =============================================================================
    // View용 DTO 클래스들
    // =============================================================================

    /**
     * 공지사항 아이템
     */
    public static class NoticeItem {
        private String cltrNo;
        private String title;
        private LocalDateTime date;
        
        public String getCltrNo() { return cltrNo; }
        public void setCltrNo(String cltrNo) { this.cltrNo = cltrNo; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
    }

    /**
     * 경매일정 아이템
     */
    public static class ScheduleItem {
        private String cltrNo;
        private String region;
        private String pbctClsDtm;
        
        public String getCltrNo() { return cltrNo; }
        public void setCltrNo(String cltrNo) { this.cltrNo = cltrNo; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getPbctClsDtm() { return pbctClsDtm; }
        public void setPbctClsDtm(String pbctClsDtm) { this.pbctClsDtm = pbctClsDtm; }
    }

    /**
     * 할인 물건 아이템
     */
    public static class DiscountItem {
        private String cltrNo;
        private String name;
        private Long startPrice;
        private Long endPrice;
        private Boolean apiItem;
        private Long no;
        
        public String getCltrNo() { return cltrNo; }
        public void setCltrNo(String cltrNo) { this.cltrNo = cltrNo; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getStartPrice() { return startPrice; }
        public void setStartPrice(Long startPrice) { this.startPrice = startPrice; }
        public Long getEndPrice() { return endPrice; }
        public void setEndPrice(Long endPrice) { this.endPrice = endPrice; }
        public Boolean getApiItem() { return apiItem; }
        public void setApiItem(Boolean apiItem) { this.apiItem = apiItem; }
        public Long getNo() { return no; }
        public void setNo(Long no) { this.no = no; }
    }

    /**
     * API 상세 페이지용 데이터 DTO
     */
    public static class ApiDetailData {
        private ItemDetail itemHistory;
        private String normalizedAddress;
        private Boolean isRealEstate;
        
        public ItemDetail getItemHistory() { return itemHistory; }
        public void setItemHistory(ItemDetail itemHistory) { this.itemHistory = itemHistory; }
        public String getNormalizedAddress() { return normalizedAddress; }
        public void setNormalizedAddress(String normalizedAddress) { this.normalizedAddress = normalizedAddress; }
        public Boolean getIsRealEstate() { return isRealEstate; }
        public void setIsRealEstate(Boolean isRealEstate) { this.isRealEstate = isRealEstate; }
    }
}

