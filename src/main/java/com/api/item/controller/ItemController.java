package com.api.item.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.api.item.service.ItemService;
import com.api.item.service.ItemRestService;
import com.api.item.service.ItemService.ApiDetailData;
import com.api.item.service.ItemService.DiscountItem;
import com.api.item.service.ItemService.NoticeItem;
import com.api.item.service.ItemService.ScheduleItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemViewService;
    private final ItemRestService itemRestService;
    
    @Data
    @AllArgsConstructor
    public static class CategoryStat {
        private String name;
        private String fullName;
        private Integer count;
    }

    /**
     * 메인 페이지
     * /main 또는 / 경로로 접근 시 main.html을 반환
     * ItemViewService를 통해 View용 데이터를 조회하여 화면에 표시
     */
    @GetMapping({"/", "/main"})
    public String mainPage(Model model) {
        log.info("🌐 메인 페이지 접근");
        
        try {
            // 1. 신규물건 공지
            List<NoticeItem> notices = itemViewService.getMainPageNotices(5);
            model.addAttribute("notices", notices);
            log.info("📊 신규물건 공지: {}개", notices.size());
            
            // 2. 용도별 물건정보 통계
            Map<String, Integer> categoryStatsMap = itemViewService.getMainPageCategoryStats();
            // 템플릿에서 쉽게 사용할 수 있도록 DTO 리스트로 변환
            List<CategoryStat> categoryStats = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : categoryStatsMap.entrySet()) {
                String fullName = entry.getKey();
                // DB의 실제 asset_category 형식: "상가용및업무용건물 / 근린생활시설" -> "근린생활시설"
                String name = fullName;
                if (fullName.contains(" / ")) {
                    String[] parts = fullName.split(" / ");
                    name = parts[parts.length - 1]; // 마지막 부분만 표시 (중분류)
                }
                categoryStats.add(new CategoryStat(name, fullName, entry.getValue()));
            }
            model.addAttribute("categoryStats", categoryStats);
            log.info("📊 카테고리 통계: {}개 카테고리", categoryStats.size());
            
            // 3. 금주의 경매일정
            List<ScheduleItem> scheduleList = itemViewService.getMainPageScheduleList(10);
            model.addAttribute("scheduleList", scheduleList);
            log.info("📊 오늘 마감하는 경매일정: {}개", scheduleList.size());
            
            // 4. 50% 체감 물건 목록
            List<DiscountItem> discountList = itemViewService.getMainPageDiscountList(4);
            model.addAttribute("discountList", discountList);
            log.info("📊 50% 체감 물건: {}개", discountList.size());
            
            log.info("✅ 메인 페이지 데이터 로드 완료: notices={}, categoryStats={}, scheduleList={}, discountList={}", 
                notices.size(), categoryStats.size(), scheduleList.size(), discountList.size());
            
        } catch (Exception e) {
            log.error("❌ 메인 페이지 데이터 로드 실패", e);
            // 에러 발생 시 빈 리스트로 설정
            model.addAttribute("notices", new ArrayList<>());
            model.addAttribute("categoryStats", new ArrayList<>());
            model.addAttribute("scheduleList", new ArrayList<>());
            model.addAttribute("discountList", new ArrayList<>());
        }
        
        return "main";
    }

    /**
     * API 상세 페이지
     * itemId 또는 cltrNo로 물건 상세 정보를 조회하여 표시
     * 기존 URL 패턴(/api-item-detail)과 새로운 URL 패턴(/item/api-detail) 모두 지원
     */
    @GetMapping({"/api-item-detail", "/item/api-detail"})
    public String apiDetailPage(
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo,
            Model model) {
        log.info("🌐 API 상세 페이지 접근: itemId={}, cltrNo={}", itemId, cltrNo);
        
        try {
            ApiDetailData detailData;
            
            if (itemId != null) {
                detailData = itemViewService.getApiDetailData(itemId);
            } else if (cltrNo != null && !cltrNo.trim().isEmpty()) {
                detailData = itemViewService.getApiDetailDataByCltrNo(cltrNo);
            } else {
                log.warn("⚠️ itemId와 cltrNo가 모두 없습니다.");
                detailData = new ApiDetailData();
            }
            
            // 모델에 데이터 추가
            model.addAttribute("itemHistory", detailData.getItemHistory());
            model.addAttribute("normalizedAddress", detailData.getNormalizedAddress());
            model.addAttribute("isRealEstate", detailData.getIsRealEstate());
            
            log.info("✅ API 상세 페이지 데이터 로드 완료: itemHistory={}, normalizedAddress={}, isRealEstate={}", 
                detailData.getItemHistory() != null, 
                detailData.getNormalizedAddress() != null ? detailData.getNormalizedAddress() : "null",
                detailData.getIsRealEstate());
            
        } catch (Exception e) {
            log.error("❌ API 상세 페이지 데이터 로드 실패", e);
            model.addAttribute("itemHistory", null);
            model.addAttribute("normalizedAddress", "");
            model.addAttribute("isRealEstate", false);
        }
        
        return "item/api-detail";
    }

    /**
     * 경매물건 목록 페이지
     */
    @GetMapping("/auctionList")
    public String auctionListPage(
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "sido", required = false) String sido,
            @RequestParam(name = "district", required = false) String district,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            Model model) {
        log.info("🌐 경매물건 목록 페이지 접근: category={}, sido={}, district={}, keyword={}, pageNum={}, pageSize={}", 
            category, sido, district, keyword, pageNum, pageSize);
        
        // 비동기 로딩을 위해 초기에는 빈 데이터로 렌더링
        // 실제 데이터는 JavaScript에서 AJAX로 로드
        String sidoParam = (sido != null && !sido.trim().isEmpty()) ? sido : "서울특별시";
        
        // 임시로 서버 사이드에서 직접 데이터 로드 (디버깅용)
        try {
            log.info("🔵 [임시] 서버 사이드에서 직접 데이터 로드 시도: category={}, sido={}, pageNum={}, pageSize={}", 
                category, sidoParam, pageNum, pageSize);
            
            // API는 200건씩 반환하므로, pageNum에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (pageNum - 1) * pageSize / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            log.info("🔵 [임시] API 페이지 계산: pageNum={}, pageSize={}, apiPage={}", pageNum, pageSize, apiPage);
            
            List<com.api.item.dto.ItemDetail> itemDetails = itemRestService.fetchAllItemsFromApi(apiPage, sidoParam);
            log.info("🔵 [임시] API 호출 완료: itemDetails.size()={} (apiPage={})", 
                itemDetails != null ? itemDetails.size() : 0, apiPage);
            
            if (itemDetails == null || itemDetails.isEmpty()) {
                log.warn("⚠️ [임시] API 응답이 비어있음");
                model.addAttribute("atList", new ArrayList<>());
                model.addAttribute("totalCount", 0);
            } else {
                // 카테고리 필터링
                if (category != null && !category.trim().isEmpty() && !category.equals("all")) {
                    int beforeSize = itemDetails.size();
                    itemDetails = itemDetails.stream()
                        .filter(item -> {
                            if (item == null) return false;
                            String assetCategory = item.getAssetCategory();
                            if (assetCategory == null) return false;
                            return assetCategory.contains(category) || category.contains(assetCategory);
                        })
                        .collect(java.util.stream.Collectors.toList());
                    log.info("🔵 [임시] 카테고리 필터링: {}개 -> {}개", beforeSize, itemDetails.size());
                }
                
                // 페이지네이션 처리 (API에서 가져온 200건 중에서 클라이언트 페이지에 맞는 부분만 추출)
                int startIndex = ((pageNum - 1) * pageSize) % 200;
                int endIndex = Math.min(startIndex + pageSize, itemDetails.size());
                List<com.api.item.dto.ItemDetail> pagedItems;
                if (startIndex < itemDetails.size()) {
                    pagedItems = itemDetails.subList(startIndex, endIndex);
                } else {
                    pagedItems = new ArrayList<>();
                }
                
                log.info("🔵 [임시] 페이지네이션 처리: startIndex={}, endIndex={}, pagedItems.size()={}", 
                    startIndex, endIndex, pagedItems.size());
                
                // 템플릿용 리스트 변환
                List<Map<String, Object>> atList = convertToAtList(pagedItems);
                log.info("🔵 [임시] convertToAtList 완료: atList.size()={}", atList != null ? atList.size() : 0);
                
                // 총 개수는 실제 API 응답 개수 사용 (추정값 대신)
                // API는 정확한 총 개수를 반환하지 않으므로, 현재 페이지의 데이터 개수로 추정
                int totalCount = itemDetails.size();
                if (itemDetails.size() == 200) {
                    // 200건이면 다음 페이지가 있을 수 있으므로 추정값 사용
                    totalCount = apiPage * 200;
                }
                
                model.addAttribute("atList", atList != null ? atList : new ArrayList<>());
                model.addAttribute("totalCount", totalCount);
                log.info("🔵 [임시] 서버 사이드 데이터 로드 완료: atList.size()={}, totalCount={}", 
                    atList != null ? atList.size() : 0, totalCount);
            }
        } catch (Exception e) {
            log.error("❌ [임시] 서버 사이드 데이터 로드 실패: {}", e.getMessage(), e);
            model.addAttribute("atList", new ArrayList<>());
            model.addAttribute("totalCount", 0);
        }
        
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageCount", (int) Math.ceil((double) (model.getAttribute("totalCount") != null ? ((Integer) model.getAttribute("totalCount")) : 0) / pageSize));
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);
        model.addAttribute("category", category);
        model.addAttribute("sido", sidoParam);
        model.addAttribute("district", district);
        model.addAttribute("keyword", keyword);
        model.addAttribute("asyncLoad", true);  // 비동기 로딩 플래그
        
        log.info("🌐 경매물건 목록 페이지 렌더링 (서버 사이드 + 비동기 로딩): category={}, pageNum={}, atList.size()={}", 
            category, pageNum, model.getAttribute("atList") != null ? ((List<?>) model.getAttribute("atList")).size() : 0);
        
        return "item/list";
    }

    /**
     * 신규물건 목록 페이지
     */
    @GetMapping("/new-items")
    public String newItemsPage(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "sido", required = false) String sido,
            Model model) {
        log.info("🌐 신규물건 목록 페이지 접근: pageNum={}, pageSize={}, sido={}", pageNum, pageSize, sido);
        
        // 비동기 로딩을 위해 초기에는 빈 데이터로 렌더링
        // 실제 데이터는 JavaScript에서 AJAX로 로드
        String sidoParam = (sido != null && !sido.trim().isEmpty()) ? sido : "서울특별시";
        
        // 임시로 서버 사이드에서 직접 데이터 로드 (디버깅용)
        try {
            log.info("🟢 [임시] 신규물건 서버 사이드에서 직접 데이터 로드 시도: sido={}", sidoParam);
            int apiPage = 1;
            List<com.api.item.dto.ItemDetail> itemDetails = itemRestService.fetchNewItemsFromApi(apiPage, sidoParam);
            log.info("🟢 [임시] 신규물건 API 호출 완료: itemDetails.size()={}", itemDetails != null ? itemDetails.size() : 0);
            
            // 페이지네이션 처리
            int startIndex = ((pageNum - 1) * pageSize) % 200;
            int endIndex = Math.min(startIndex + pageSize, itemDetails.size());
            List<com.api.item.dto.ItemDetail> pagedItems;
            if (startIndex < itemDetails.size()) {
                pagedItems = itemDetails.subList(startIndex, endIndex);
            } else {
                pagedItems = new ArrayList<>();
            }
            
            // 템플릿용 리스트 변환
            List<Map<String, Object>> atList = convertToAtList(pagedItems);
            log.info("🟢 [임시] 신규물건 convertToAtList 완료: atList.size()={}", atList != null ? atList.size() : 0);
            
            // 총 개수는 추정값
            int totalCount = pagedItems.size() == pageSize ? (apiPage * 200) : ((apiPage - 1) * 200 + pagedItems.size());
            
            model.addAttribute("atList", atList != null ? atList : new ArrayList<>());
            model.addAttribute("totalCount", totalCount);
            log.info("🟢 [임시] 신규물건 서버 사이드 데이터 로드 완료: atList.size()={}, totalCount={}", 
                atList != null ? atList.size() : 0, totalCount);
        } catch (Exception e) {
            log.error("❌ [임시] 신규물건 서버 사이드 데이터 로드 실패: {}", e.getMessage(), e);
            model.addAttribute("atList", new ArrayList<>());
            model.addAttribute("totalCount", 0);
        }
        
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageCount", (int) Math.ceil((double) (model.getAttribute("totalCount") != null ? ((Integer) model.getAttribute("totalCount")) : 0) / pageSize));
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);
        model.addAttribute("category", "신규물건");
        model.addAttribute("isNew", true);
        model.addAttribute("sido", sidoParam);
        model.addAttribute("asyncLoad", true);  // 비동기 로딩 플래그
        
        log.info("🌐 신규물건 목록 페이지 렌더링 (서버 사이드 + 비동기 로딩): pageNum={}, atList.size()={}", 
            pageNum, model.getAttribute("atList") != null ? ((List<?>) model.getAttribute("atList")).size() : 0);
        
        return "item/list";
    }

    /**
     * 50% 체감 물건 목록 페이지
     */
    @GetMapping("/discount-50")
    public String discount50Page(
            @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(name = "sido", required = false) String sido,
            Model model) {
        log.info("🌐 50% 체감 물건 목록 페이지 접근: pageNum={}, pageSize={}, sido={}", pageNum, pageSize, sido);
        
        String sidoParam = (sido != null && !sido.trim().isEmpty()) ? sido : "서울특별시";
        
        // 임시로 서버 사이드에서 직접 데이터 로드 (디버깅용)
        try {
            log.info("🟡 [임시] 50% 체감물건 서버 사이드에서 직접 데이터 로드 시도: sido={}", sidoParam);
            int apiPage = 1;
            List<com.api.item.dto.ItemDetail> itemDetails = itemRestService.fetchDiscountItemsFromApi(apiPage, sidoParam);
            log.info("🟡 [임시] 50% 체감물건 API 호출 완료: itemDetails.size()={}", itemDetails != null ? itemDetails.size() : 0);
            
            // 페이지네이션 처리
            int startIndex = ((pageNum - 1) * pageSize) % 200;
            int endIndex = Math.min(startIndex + pageSize, itemDetails.size());
            List<com.api.item.dto.ItemDetail> pagedItems;
            if (startIndex < itemDetails.size()) {
                pagedItems = itemDetails.subList(startIndex, endIndex);
            } else {
                pagedItems = new ArrayList<>();
            }
            
            // 템플릿용 리스트 변환
            List<Map<String, Object>> atList = convertToAtList(pagedItems);
            log.info("🟡 [임시] 50% 체감물건 convertToAtList 완료: atList.size()={}", atList != null ? atList.size() : 0);
            
            // 총 개수는 추정값
            int totalCount = pagedItems.size() == pageSize ? (apiPage * 200) : ((apiPage - 1) * 200 + pagedItems.size());
            
            model.addAttribute("atList", atList != null ? atList : new ArrayList<>());
            model.addAttribute("totalCount", totalCount);
            log.info("🟡 [임시] 50% 체감물건 서버 사이드 데이터 로드 완료: atList.size()={}, totalCount={}", 
                atList != null ? atList.size() : 0, totalCount);
        } catch (Exception e) {
            log.error("❌ [임시] 50% 체감물건 서버 사이드 데이터 로드 실패: {}", e.getMessage(), e);
            model.addAttribute("atList", new ArrayList<>());
            model.addAttribute("totalCount", 0);
        }
        
        model.addAttribute("pageNum", pageNum);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("pageCount", (int) Math.ceil((double) (model.getAttribute("totalCount") != null ? ((Integer) model.getAttribute("totalCount")) : 0) / pageSize));
        model.addAttribute("startPage", 1);
        model.addAttribute("endPage", 1);
        model.addAttribute("category", "50% 체감 물건");
        model.addAttribute("discountFilter", true);
        model.addAttribute("isDiscount", true);
        model.addAttribute("sido", sidoParam);
        model.addAttribute("asyncLoad", true);  // 비동기 로딩 플래그
        
        log.info("🌐 50% 체감 물건 목록 페이지 렌더링 (서버 사이드 + 비동기 로딩): pageNum={}, atList.size()={}", 
            pageNum, model.getAttribute("atList") != null ? ((List<?>) model.getAttribute("atList")).size() : 0);
        
        return "item/list";
    }
    
    /**
     * ItemDetail 리스트를 템플릿용 Map 리스트로 변환
     * 템플릿에서 사용하는 필드명에 맞춰 매핑
     */
    private List<Map<String, Object>> convertToAtList(List<com.api.item.dto.ItemDetail> itemDetails) {
        List<Map<String, Object>> atList = new ArrayList<>();
        
        if (itemDetails == null || itemDetails.isEmpty()) {
            return atList;
        }
        
        for (com.api.item.dto.ItemDetail item : itemDetails) {
            Map<String, Object> map = new HashMap<>();
            
            // 템플릿에서 사용하는 필드명으로 매핑
            map.put("cltrNo", item.getCltrMnmtNo() != null ? item.getCltrMnmtNo() : "");
            map.put("name", item.getAddress() != null ? item.getAddress() : "");
            map.put("content", item.getGoodsDetail() != null ? item.getGoodsDetail() : "");
            
            // 유찰 횟수 매핑 (디버깅용 로그 추가)
            Integer bidCount = item.getBidCount();
            if (bidCount == null) {
                bidCount = 0;
            }
            map.put("count", bidCount);
            
            // 디버깅: bidCount가 0이 아닌 경우 로그 출력
            if (bidCount > 0) {
                log.debug("📊 유찰 횟수 반영: cltrNo={}, bidCount={}", 
                    item.getCltrMnmtNo(), bidCount);
            }
            
            // 가격 정보 (템플릿에서 startPrice, endPrice 사용)
            map.put("startPrice", item.getMinBidPriceMin() != null ? item.getMinBidPriceMin() : 0L);
            map.put("endPrice", item.getAppraisalAmountMax() != null ? item.getAppraisalAmountMax() : 0L);
            
            // 날짜 정보 (템플릿에서 endDate 사용)
            // 유효한 날짜 범위 확인: 2020년 ~ 현재 연도까지만 허용 (미래 날짜 제한)
            LocalDateTime bidEnd = item.getBidEnd();
            if (bidEnd != null) {
                int year = bidEnd.getYear();
                int currentYear = LocalDateTime.now().getYear();
                // 2020년 이전이거나, 현재 연도를 초과하는 경우 null로 설정
                if (year < 2020 || year > currentYear) {
                    log.debug("⚠️ 유효하지 않은 날짜 필터링: plnmNo={}, bidEnd={}, year={}, currentYear={}", 
                        item.getPlnmNo(), bidEnd, year, currentYear);
                    bidEnd = null;
                }
            }
            
            // bidStart도 검증
            LocalDateTime bidStart = item.getBidStart();
            if (bidStart != null) {
                int year = bidStart.getYear();
                int currentYear = LocalDateTime.now().getYear();
                // 2020년 이전이거나, 현재 연도를 초과하는 경우 null로 설정
                if (year < 2020 || year > currentYear) {
                    log.debug("⚠️ 유효하지 않은 날짜 필터링: plnmNo={}, bidStart={}, year={}, currentYear={}", 
                        item.getPlnmNo(), bidStart, year, currentYear);
                    bidStart = null;
                }
            }
            
            map.put("endDate", bidEnd);
            map.put("bidEnd", bidEnd); // 호환성 유지
            map.put("startDate", bidStart);
            map.put("bidStart", bidStart); // 호환성 유지
            
            // 기타 정보
            map.put("orgName", item.getOrgName() != null ? item.getOrgName() : "");
            map.put("plnmNo", item.getPlnmNo());
            
            // 호환성을 위한 추가 필드
            map.put("appraisalAmount", item.getAppraisalAmountMax() != null ? item.getAppraisalAmountMax() : 0L);
            map.put("minBidPrice", item.getMinBidPriceMin() != null ? item.getMinBidPriceMin() : 0L);
            
            atList.add(map);
        }
        
        // 유찰 횟수가 0보다 큰 아이템 개수 확인
        long itemsWithCount = atList.stream()
            .filter(map -> {
                Object countObj = map.get("count");
                if (countObj instanceof Integer) {
                    return ((Integer) countObj) > 0;
                }
                return false;
            })
            .count();
        
        log.info("📋 convertToAtList: {}개 아이템 변환 완료, 유찰 횟수 > 0인 아이템: {}개", 
            atList.size(), itemsWithCount);
        
        // 유찰 횟수가 있는 아이템 샘플 로그
        atList.stream()
            .filter(map -> {
                Object countObj = map.get("count");
                if (countObj instanceof Integer) {
                    return ((Integer) countObj) > 0;
                }
                return false;
            })
            .limit(5)
            .forEach(map -> log.info("📊 convertToAtList 유찰 횟수 샘플: cltrNo={}, count={}", 
                map.get("cltrNo"), map.get("count")));
        
        return atList;
    }
    
    /**
     * 물건 상세보기 (기존 URL 호환성)
     * /detail로 접근 시 /api-item-detail로 리다이렉트
     */
    @GetMapping("/detail")
    public String detailPage(
            @RequestParam(name = "itemId", required = false) Long itemId,
            @RequestParam(name = "cltrNo", required = false) String cltrNo) {
        log.info("🌐 물건 상세보기 리다이렉트: itemId={}, cltrNo={}", itemId, cltrNo);
        if (itemId != null) {
            return "redirect:/api-item-detail?itemId=" + itemId;
        } else if (cltrNo != null && !cltrNo.trim().isEmpty()) {
            return "redirect:/api-item-detail?cltrNo=" + cltrNo;
        }
        return "redirect:/auctionList";
    }
}

