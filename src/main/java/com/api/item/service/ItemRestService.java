package com.api.item.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.api.item.dto.ItemBasic;
import com.api.item.dto.ItemDetail;
import com.api.item.mapper.ItemMapper;
import com.api.union.service.ApiService;
import com.api.util.ApiXmlParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemRestService {

	private final ItemMapper mapper;
	private final ApiService apiService;
	
	private static final int PAGE_SIZE = 200; // 페이지당 200건
	// 전체 목록 조회 (API에서 조회)
    public List<ItemBasic> getItemList() {
        try {
            List<ItemDetail> details = fetchAllItemsFromApi(1, "서울특별시");
            return details.stream()
                .map(this::convertDetailToBasic)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("❌ 전체 목록 조회 실패", e);
            return List.of();
        }
    }

    // 신규 목록 조회 (API에서 조회)
    public List<ItemBasic> getNewItems() {
        try {
            List<ItemDetail> details = fetchNewItemsFromApi(1, "서울특별시");
            if (details.size() > 50) {
                details = details.subList(0, 50);
            }
            return details.stream()
                .map(this::convertDetailToBasic)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("❌ 신규 목록 조회 실패", e);
            return List.of();
        }
    }

    // 할인 목록 조회 (API에서 조회)
    public List<ItemBasic> getDiscountItems() {
        try {
            List<ItemDetail> details = fetchDiscountItemsFromApi(1, "서울특별시");
            if (details.size() > 50) {
                details = details.subList(0, 50);
            }
            return details.stream()
                .map(this::convertDetailToBasic)
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.error("❌ 할인 목록 조회 실패", e);
            return List.of();
        }
    }
    
    // ItemDetail을 ItemBasic으로 변환
    private ItemBasic convertDetailToBasic(ItemDetail detail) {
        return ItemBasic.builder()
            .rnum(detail.getRnum())
            .plnmNo(detail.getPlnmNo())
            .address(detail.getAddress())
            .appraisalAmountMin(detail.getAppraisalAmountMin())
            .appraisalAmountMax(detail.getAppraisalAmountMax())
            .minBidPriceMin(detail.getMinBidPriceMin())
            .minBidPriceMax(detail.getMinBidPriceMax())
            .orgName(detail.getOrgName())
            .bidStart(detail.getBidStart())
            .bidEnd(detail.getBidEnd())
            .disposalMethod(detail.getDisposalMethod())
            .bidMethod(detail.getBidMethod())
            .bidCount(detail.getBidCount())
            .build();
    }

    // 상세 조회 (API에서 조회)
    public ItemDetail getItemDetail(Long plnmNo) {
        // plnmNo로는 API에서 직접 조회할 수 없으므로, 여러 API를 조회하여 찾기
        // 주의: 이 방법은 느릴 수 있으므로, 가능하면 cltrNo를 사용하는 것을 권장
        log.warn("⚠️ getItemDetail(plnmNo)는 API에서 직접 조회할 수 없습니다. cltrNo를 사용하는 것을 권장합니다. plnmNo={}", plnmNo);
        
        // API에서 여러 페이지를 조회하여 찾기
        try {
            // 용도별 통합 조회에서 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> plnmNo.equals(item.getPlnmNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공: plnmNo={}, page={}", plnmNo, page);
                    return found;
                }
            }
            
            // 신물건에서도 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchNewItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> plnmNo.equals(item.getPlnmNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공 (신물건): plnmNo={}, page={}", plnmNo, page);
                    return found;
                }
            }
            
            // 감가 50% 이상에서도 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchDiscountItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> plnmNo.equals(item.getPlnmNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공 (감가50%): plnmNo={}, page={}", plnmNo, page);
                    return found;
                }
            }
            
            log.warn("⚠️ API에서 물건을 찾을 수 없음: plnmNo={}", plnmNo);
        } catch (Exception e) {
            log.error("❌ API에서 물건 조회 실패: plnmNo={}", plnmNo, e);
        }
        
        return null;
    }
    
    // 상세 조회 (cltrMnmtNo로 - API에서 조회)
    public ItemDetail getItemDetailByCltrMnmtNo(String cltrMnmtNo) {
        if (cltrMnmtNo == null || cltrMnmtNo.trim().isEmpty()) {
            return null;
        }
        
        // API에서 여러 페이지를 조회하여 cltrNo로 매칭
        try {
            // 용도별 통합 조회에서 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> cltrMnmtNo.equals(item.getCltrMnmtNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공: cltrNo={}, page={}", cltrMnmtNo, page);
                    return found;
                }
            }
            
            // 신물건에서도 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchNewItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> cltrMnmtNo.equals(item.getCltrMnmtNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공 (신물건): cltrNo={}, page={}", cltrMnmtNo, page);
                    return found;
                }
            }
            
            // 감가 50% 이상에서도 찾기
            for (int page = 1; page <= 10; page++) {
                List<ItemDetail> items = fetchDiscountItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                
                ItemDetail found = items.stream()
                    .filter(item -> cltrMnmtNo.equals(item.getCltrMnmtNo()))
                    .findFirst()
                    .orElse(null);
                
                if (found != null) {
                    log.info("✅ API에서 물건 조회 성공 (감가50%): cltrNo={}, page={}", cltrMnmtNo, page);
                    return found;
                }
            }
            
            log.warn("⚠️ API에서 물건을 찾을 수 없음: cltrNo={}", cltrMnmtNo);
        } catch (Exception e) {
            log.error("❌ API에서 물건 조회 실패: cltrNo={}", cltrMnmtNo, e);
        }
        
        return null;
    }

    // 온비드 API에서 가져온 물건을 저장/갱신
    public int upsertItems(List<ItemDetail> details) {
        int saved = 0;
        if (details == null) {
            return saved;
        }

        for (ItemDetail detail : details) {
            if (detail == null || detail.getPlnmNo() == null) {
                continue;
            }
            mapper.upsertItemBasic(convertToBasic(detail));
            mapper.upsertItemDetail(detail);
            saved++;
        }
        return saved;
    }

    private ItemBasic convertToBasic(ItemDetail detail) {
        return ItemBasic.builder()
            .rnum(detail.getRnum())
            .plnmNo(detail.getPlnmNo())
            .address(detail.getAddress())
            .appraisalAmountMin(detail.getAppraisalAmountMin())
            .appraisalAmountMax(detail.getAppraisalAmountMax())
            .minBidPriceMin(detail.getMinBidPriceMin())
            .minBidPriceMax(detail.getMinBidPriceMax())
            .orgName(detail.getOrgName())
            .bidStart(detail.getBidStart())
            .bidEnd(detail.getBidEnd())
            .disposalMethod(detail.getDisposalMethod())
            .bidMethod(detail.getBidMethod())
            .bidCount(detail.getBidCount())
            .build();
    }

    // 삭제: plnmNo로 삭제 (item_detail과 item_basic 모두 삭제)
    public int deleteItemByPlnmNo(Long plnmNo) {
        // item_detail 먼저 삭제 (외래 키 제약 조건 때문에)
        int deletedDetail = mapper.deleteItemByPlnmNo(plnmNo);
        // item_basic 삭제
        int deletedBasic = mapper.deleteItemBasicByPlnmNo(plnmNo);
        return deletedDetail + deletedBasic;
    }

    // 삭제: 서울특별시가 아닌 데이터 삭제
    public int deleteNonSeoulItems() {
        return mapper.deleteNonSeoulItems();
    }

    // 삭제: 전체 삭제
    public int deleteAllItems() {
        return mapper.deleteAllItems();
    }
    
    // 조회: 서울특별시 물건 조회 (페이징) - API에서 조회
    public List<ItemDetail> getItemsSeoul(int page, int size) {
        try {
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * size / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            List<ItemDetail> itemDetails = fetchUsageItemsFromApi(apiPage, "서울특별시");
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * size) % 200;
            int endIndex = Math.min(startIndex + size, itemDetails.size());
            if (startIndex < itemDetails.size()) {
                itemDetails = itemDetails.subList(startIndex, endIndex);
            } else {
                itemDetails = new java.util.ArrayList<>();
            }
            
            return itemDetails;
        } catch (Exception e) {
            log.error("❌ 서울특별시 물건 조회 실패", e);
            return List.of();
        }
    }
    
    // 조회: 서울특별시 물건 총 개수 (API는 정확한 총 개수를 반환하지 않음)
    public int countItemsSeoul() {
        // API는 정확한 총 개수를 반환하지 않으므로 추정값 반환
        return 10000; // 추정값
    }
    
    // 조회: 전체 물건 조회 (페이징) - API에서 조회
    public List<ItemDetail> getAllItems(int page, int size, String category) {
        try {
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * size / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            List<ItemDetail> itemDetails = fetchAllItemsFromApi(apiPage, "서울특별시");
            
            // 카테고리 필터링
            if (category != null && !category.trim().isEmpty() && !category.equals("all")) {
                itemDetails = itemDetails.stream()
                    .filter(item -> {
                        String assetCategory = item.getAssetCategory();
                        if (assetCategory == null) return false;
                        return assetCategory.contains(category) || category.contains(assetCategory);
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * size) % 200;
            int endIndex = Math.min(startIndex + size, itemDetails.size());
            if (startIndex < itemDetails.size()) {
                itemDetails = itemDetails.subList(startIndex, endIndex);
            } else {
                itemDetails = new java.util.ArrayList<>();
            }
            
            return itemDetails;
        } catch (Exception e) {
            log.error("❌ 전체 물건 조회 실패", e);
            return List.of();
        }
    }
    
    // 조회: 전체 물건 총 개수 (API는 정확한 총 개수를 반환하지 않음)
    public int countAllItems(String category) {
        // API는 정확한 총 개수를 반환하지 않으므로 추정값 반환
        return 10000; // 추정값
    }
    
    // 삭제: ID(plnmNo)로 삭제
    public int deleteItemById(Long id) {
        return mapper.deleteItemById(id);
    }
    
    // 삭제: 물건번호(cltrMnmtNo)로 삭제
    public int deleteItemByCltrNo(String cltrNo) {
        return mapper.deleteItemByCltrNo(cltrNo);
    }
    
    // 조회: 신규 물건 조회 (페이징) - API에서 조회
    public List<ItemDetail> getNewItemsDetail(int page, int size) {
        try {
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * size / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            List<ItemDetail> itemDetails = fetchNewItemsFromApi(apiPage, "서울특별시");
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * size) % 200;
            int endIndex = Math.min(startIndex + size, itemDetails.size());
            if (startIndex < itemDetails.size()) {
                itemDetails = itemDetails.subList(startIndex, endIndex);
            } else {
                itemDetails = new java.util.ArrayList<>();
            }
            
            return itemDetails;
        } catch (Exception e) {
            log.error("❌ 신규 물건 조회 실패", e);
            return List.of();
        }
    }
    
    // 조회: 신규 물건 총 개수 (API는 정확한 총 개수를 반환하지 않음)
    public int countNewItems() {
        // API는 정확한 총 개수를 반환하지 않으므로 추정값 반환
        return 5000; // 추정값
    }
    
    // 조회: 감가 50% 이상 물건 조회 (페이징) - API에서 조회
    public List<ItemDetail> getDiscountItemsDetail(int page, int size) {
        try {
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * size / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            List<ItemDetail> itemDetails = fetchDiscountItemsFromApi(apiPage, "서울특별시");
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * size) % 200;
            int endIndex = Math.min(startIndex + size, itemDetails.size());
            if (startIndex < itemDetails.size()) {
                itemDetails = itemDetails.subList(startIndex, endIndex);
            } else {
                itemDetails = new java.util.ArrayList<>();
            }
            
            return itemDetails;
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 물건 조회 실패", e);
            return List.of();
        }
    }
    
    // 조회: 감가 50% 이상 물건 총 개수 (API는 정확한 총 개수를 반환하지 않음)
    public int countDiscountItems() {
        // API는 정확한 총 개수를 반환하지 않으므로 추정값 반환
        return 3000; // 추정값
    }
    
    // 조회: 오늘 마감하는 물건 조회 (경매일정용) - API에서 조회
    public List<ItemDetail> getTodayClosingItems(int limit) {
        try {
            List<ItemDetail> todayItems = new java.util.ArrayList<>();
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime todayStart = today.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime todayEnd = today.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            
            // 최대 3페이지 조회하여 오늘 마감하는 물건 찾기
            for (int page = 1; page <= 3; page++) {
                List<ItemDetail> items = fetchUsageItemsFromApi(page, "서울특별시");
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
            
            return todayItems;
        } catch (Exception e) {
            log.error("❌ 오늘 마감하는 물건 조회 실패", e);
            return List.of();
        }
    }
    
    // 조회: 카테고리별 통계 - API에서 조회
    public List<Map<String, Object>> getCategoryStats() {
        try {
            List<ItemDetail> allItems = new java.util.ArrayList<>();
            for (int page = 1; page <= 3; page++) {
                List<ItemDetail> items = fetchUsageItemsFromApi(page, "서울특별시");
                if (items.isEmpty()) break;
                allItems.addAll(items);
            }
            
            // 카테고리별로 그룹화
            Map<String, Long> categoryCounts = allItems.stream()
                .filter(item -> item.getAssetCategory() != null && !item.getAssetCategory().trim().isEmpty())
                .filter(item -> !item.getAssetCategory().contains("기타") && !item.getAssetCategory().contains("미분류"))
                .collect(java.util.stream.Collectors.groupingBy(
                    ItemDetail::getAssetCategory,
                    java.util.stream.Collectors.counting()
                ));
            
            // Map<String, Object> 리스트로 변환
            List<Map<String, Object>> stats = new java.util.ArrayList<>();
            categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(12) // 최대 12개만
                .forEach(entry -> {
                    Map<String, Object> stat = new java.util.HashMap<>();
                    stat.put("category", entry.getKey());
                    stat.put("count", entry.getValue().intValue());
                    stats.add(stat);
                });
            
            return stats;
        } catch (Exception e) {
            log.error("❌ 카테고리 통계 조회 실패", e);
            return List.of();
        }
    }
    
    // =============================================================================
    // API 호출 페이지네이션 기능 (200건씩) - Union 패키지와 동일
    // =============================================================================
    
    /**
     * 신물건 조회 (페이지네이션) - 200건씩
     * 캐싱: 5분 (300초) - 동일한 page, sido 조합에 대해 캐시 사용
     */
    // 캐시 비활성화 (디버깅용) - 문제 해결 후 다시 활성화
    // @Cacheable(value = "apiNewItems", key = "#page + '_' + #sido", unless = "#result == null or #result.isEmpty()")
    public List<ItemDetail> fetchNewItemsFromApi(int page, String sido) {
        try {
            log.info("🟢 [신물건] API 호출 시작: page={}, sido={}, PAGE_SIZE={}", page, sido, PAGE_SIZE);
            System.out.println("🟢 [신물건] fetchNewItemsFromApi 호출됨 - getUnifyNewCltrList 사용");
            String xml = apiService.getUnifyNewCltrList(page, PAGE_SIZE, sido);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("🟢 [신물건] ItemService 신물건 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            
            if (details == null || details.isEmpty()) {
                return List.of();
            }
            
            // 같은 물건 ID(plnmNo 또는 cltrMnmtNo)가 여러 번 나오면 최신 것만 표시하고 나머지는 유찰 횟수로 카운트
            try {
                List<ItemDetail> processedDetails = processDuplicateItems(details);
                log.info("🟢 중복 제거 후 신물건: {}개 (원본: {}개)", processedDetails.size(), details.size());
                
                // 유찰 횟수가 있는 아이템 개수 확인
                long itemsWithBidCount = processedDetails.stream()
                    .filter(item -> item != null && item.getBidCount() != null && item.getBidCount() > 0)
                    .count();
                log.info("🟢 신물건 유찰 횟수 > 0인 아이템: {}개", itemsWithBidCount);
                
                return processedDetails;
            } catch (Exception e) {
                log.error("❌ 중복 제거 처리 중 오류 발생, 원본 데이터 반환: {}", e.getMessage(), e);
                // 오류 발생 시에도 bidCount 초기화
                for (ItemDetail item : details) {
                    if (item != null && item.getBidCount() == null) {
                        item.setBidCount(0);
                    }
                }
                return details; // 오류 발생 시 원본 데이터 반환
            }
        } catch (Exception e) {
            log.error("❌ ItemService 신물건 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 감가 50% 이상 조회 (페이지네이션) - 200건씩
     */
    public List<ItemDetail> fetchDiscountItemsFromApi(int page, String sido) {
        try {
            log.info("🟡 [50% 체감물건] API 호출 시작: page={}, sido={}, PAGE_SIZE={}", page, sido, PAGE_SIZE);
            System.out.println("🟡 [50% 체감물건] fetchDiscountItemsFromApi 호출됨 - getUnifyDegression50PerCltrList 사용");
            
            String xml = apiService.getUnifyDegression50PerCltrList(page, PAGE_SIZE, sido);
            log.info("🟡 [50% 체감물건] API 호출 완료: xml != null = {}", xml != null);
            
            if (xml == null || xml.trim().isEmpty()) {
                log.warn("⚠️ 50% 체감물건 API 응답이 비어있음: page={}, sido={}", page, sido);
                return List.of();
            }
            
            log.info("📄 50% 체감물건 API XML 응답 길이: {} bytes", xml.length());
            if (xml.length() < 500) {
                log.warn("⚠️ 50% 체감물건 API XML 응답이 너무 짧음: {}", xml);
            }
            
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("🟡 [50% 체감물건] ItemService 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            
            if (details == null || details.isEmpty()) {
                log.warn("⚠️ 50% 체감물건 파싱 결과가 비어있음: page={}, sido={}, xmlLength={}", page, sido, xml.length());
                return List.of();
            }
            
            // 같은 물건 ID(plnmNo 또는 cltrMnmtNo)가 여러 번 나오면 최신 것만 표시하고 나머지는 유찰 횟수로 카운트
            try {
                List<ItemDetail> processedDetails = processDuplicateItems(details);
                log.info("🟡 중복 제거 후 50% 체감물건: {}개 (원본: {}개)", processedDetails.size(), details.size());
                
                // 유찰 횟수가 있는 아이템 개수 확인
                long itemsWithBidCount = processedDetails.stream()
                    .filter(item -> item != null && item.getBidCount() != null && item.getBidCount() > 0)
                    .count();
                log.info("🟡 50% 체감물건 유찰 횟수 > 0인 아이템: {}개", itemsWithBidCount);
                
                return processedDetails;
            } catch (Exception e) {
                log.error("❌ 중복 제거 처리 중 오류 발생, 원본 데이터 반환: {}", e.getMessage(), e);
                // 오류 발생 시에도 bidCount 초기화
                for (ItemDetail item : details) {
                    if (item != null && item.getBidCount() == null) {
                        item.setBidCount(0);
                    }
                }
                return details; // 오류 발생 시 원본 데이터 반환
            }
        } catch (Exception e) {
            log.error("❌ ItemService 50% 체감물건 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 전체 경매물건 조회 (페이지네이션) - 200건씩
     * 용도별 통합 조회 API 사용
     * 캐싱: 5분 (300초) - 동일한 page, sido 조합에 대해 캐시 사용
     */
    // 캐시 비활성화 (디버깅용) - 문제 해결 후 다시 활성화
    // @Cacheable(value = "apiItems", key = "#page + '_' + #sido", unless = "#result == null or #result.isEmpty()")
    public List<ItemDetail> fetchAllItemsFromApi(int page, String sido) {
        try {
            log.info("🔵 [전체 경매물건] API 호출 시작: page={}, sido={}, PAGE_SIZE={}", page, sido, PAGE_SIZE);
            System.out.println("🔵 [전체 경매물건] fetchAllItemsFromApi 호출됨 - getUnifyUsageCltrList 사용");
            
            String xml = apiService.getUnifyUsageCltrList(page, PAGE_SIZE, sido);
            log.info("🔵 [전체 경매물건] API 호출 완료: xml != null = {}", xml != null);
            
            if (xml == null || xml.trim().isEmpty()) {
                log.warn("⚠️ 전체 경매물건 API 응답이 비어있음: page={}, sido={}", page, sido);
                return List.of();
            }
            
            log.info("📄 전체 경매물건 API XML 응답 길이: {} bytes", xml.length());
            if (xml.length() < 500) {
                log.warn("⚠️ 전체 경매물건 API XML 응답이 너무 짧음: {}", xml);
            }
            
            // XML 응답의 처음 200자 로깅
            if (xml.length() > 200) {
                log.info("📄 XML 응답 시작 부분: {}", xml.substring(0, 200));
            } else {
                log.info("📄 XML 응답 전체: {}", xml);
            }
            
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("📡 ItemService 전체 경매물건 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            
            if (details == null || details.isEmpty()) {
                log.warn("⚠️ 전체 경매물건 파싱 결과가 비어있음: page={}, sido={}, xmlLength={}", page, sido, xml.length());
                // XML 내용 확인을 위해 더 많은 정보 로깅
                if (xml.contains("<resultCode>")) {
                    log.error("❌ API 에러 응답: resultCode 태그 발견");
                    // resultCode와 resultMsg 추출
                    try {
                        int resultCodeStart = xml.indexOf("<resultCode>");
                        int resultCodeEnd = xml.indexOf("</resultCode>");
                        if (resultCodeStart >= 0 && resultCodeEnd > resultCodeStart) {
                            String resultCode = xml.substring(resultCodeStart + 13, resultCodeEnd);
                            log.error("❌ resultCode: {}", resultCode);
                        }
                        int resultMsgStart = xml.indexOf("<resultMsg>");
                        int resultMsgEnd = xml.indexOf("</resultMsg>");
                        if (resultMsgStart >= 0 && resultMsgEnd > resultMsgStart) {
                            String resultMsg = xml.substring(resultMsgStart + 11, resultMsgEnd);
                            log.error("❌ resultMsg: {}", resultMsg);
                        }
                    } catch (Exception e) {
                        log.error("❌ XML 파싱 중 오류: {}", e.getMessage());
                    }
                }
                if (xml.contains("error")) {
                    log.error("❌ API 에러 응답: error 태그 발견");
                }
                // XML의 item 개수 확인
                int itemCount = xml.split("<item>").length - 1;
                log.warn("⚠️ XML에서 <item> 태그 개수: {}개", itemCount);
                return List.of();
            }
            
            // 파싱된 아이템 개수가 예상보다 적은 경우 경고
            if (details.size() < 50) {
                log.warn("⚠️ 전체 경매물건 파싱 결과가 예상보다 적음: page={}, sido={}, count={}, xmlLength={}", 
                    page, sido, details.size(), xml.length());
                // XML의 item 개수 확인
                int itemCount = xml.split("<item>").length - 1;
                log.warn("⚠️ XML에서 <item> 태그 개수: {}개", itemCount);
            }
            
            // 같은 물건 ID(plnmNo 또는 cltrMnmtNo)가 여러 번 나오면 최신 것만 표시하고 나머지는 유찰 횟수로 카운트
            try {
                List<ItemDetail> processedDetails = processDuplicateItems(details);
                log.info("📡 중복 제거 후 전체 경매물건: {}개 (원본: {}개)", processedDetails.size(), details.size());
                
                // 유찰 횟수가 있는 아이템 개수 확인
                long itemsWithBidCount = processedDetails.stream()
                    .filter(item -> item != null && item.getBidCount() != null && item.getBidCount() > 0)
                    .count();
                log.info("📡 유찰 횟수 > 0인 아이템: {}개", itemsWithBidCount);
                
                return processedDetails;
            } catch (Exception e) {
                log.error("❌ 중복 제거 처리 중 오류 발생, 원본 데이터 반환: {}", e.getMessage(), e);
                // 오류 발생 시에도 bidCount 초기화
                for (ItemDetail item : details) {
                    if (item != null && item.getBidCount() == null) {
                        item.setBidCount(0);
                    }
                }
                return details; // 오류 발생 시 원본 데이터 반환
            }
        } catch (Exception e) {
            log.error("❌ ItemService 전체 경매물건 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 용도별 통합 조회 (페이지네이션) - 200건씩
     * 캐싱: 5분 (300초) - 동일한 page, sido 조합에 대해 캐시 사용
     */
    @Cacheable(value = "apiUsageItems", key = "#page + '_' + #sido", unless = "#result == null or #result.isEmpty()")
    public List<ItemDetail> fetchUsageItemsFromApi(int page, String sido) {
        try {
            String xml = apiService.getUnifyUsageCltrList(page, PAGE_SIZE, sido);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("📡 ItemService 용도별 통합 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            return details != null ? details : List.of();
        } catch (Exception e) {
            log.error("❌ ItemService 용도별 통합 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 신물건 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveNewItemsFromApi(int page, String sido) {
        try {
            List<ItemDetail> details = fetchNewItemsFromApi(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = upsertItems(details);
            log.info("💾 ItemService 신물건 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ ItemService 신물건 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 감가 50% 이상 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveDiscountItemsFromApi(int page, String sido) {
        try {
            List<ItemDetail> details = fetchDiscountItemsFromApi(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = upsertItems(details);
            log.info("💾 ItemService 감가 50% 이상 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ ItemService 감가 50% 이상 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 용도별 통합 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveUsageItemsFromApi(int page, String sido) {
        try {
            List<ItemDetail> details = fetchUsageItemsFromApi(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = upsertItems(details);
            log.info("💾 ItemService 용도별 통합 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ ItemService 용도별 통합 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }
    
    // =============================================================================
    // 삭제 기능
    // =============================================================================
    
    /**
     * 신물건 삭제 (14일 이내 데이터)
     */
    public int deleteNewItems() {
        try {
            int deleted = mapper.deleteNewItems();
            log.info("🗑️ ItemService 신물건 삭제 완료: {}건", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("❌ ItemService 신물건 삭제 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 감가 50% 이상 물건 삭제
     */
    public int deleteDiscountItems() {
        try {
            int deleted = mapper.deleteDiscountItems();
            log.info("🗑️ ItemService 감가 50% 이상 물건 삭제 완료: {}건", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("❌ ItemService 감가 50% 이상 물건 삭제 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 서울특별시 전체 물건 삭제 (용도별통합물건)
     */
    public int deleteUsageItems() {
        try {
            int deleted = mapper.deleteUsageItems();
            log.info("🗑️ ItemService 용도별 통합 물건 삭제 완료: {}건", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("❌ ItemService 용도별 통합 물건 삭제 실패: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * ItemDetail 리스트를 템플릿용 Map 리스트로 변환
     */
    public List<Map<String, Object>> convertToAtList(List<ItemDetail> itemDetails) {
        List<Map<String, Object>> atList = new java.util.ArrayList<>();
        
        if (itemDetails == null || itemDetails.isEmpty()) {
            return atList;
        }
        
        for (ItemDetail item : itemDetails) {
            Map<String, Object> map = new java.util.HashMap<>();
            
            // 템플릿에서 사용하는 필드명으로 매핑
            map.put("cltrNo", item.getCltrMnmtNo() != null ? item.getCltrMnmtNo() : "");
            map.put("name", item.getAddress() != null ? item.getAddress() : "");
            map.put("content", item.getGoodsDetail() != null ? item.getGoodsDetail() : "");
            map.put("count", item.getBidCount() != null ? item.getBidCount() : 0);
            
            // 가격 정보
            map.put("startPrice", item.getMinBidPriceMin() != null ? item.getMinBidPriceMin() : 0L);
            map.put("endPrice", item.getAppraisalAmountMax() != null ? item.getAppraisalAmountMax() : 0L);
            
            // 날짜 정보 (유효한 날짜 범위 확인: 2020년 ~ 현재 연도까지만 허용)
            java.time.LocalDateTime bidEnd = item.getBidEnd();
            if (bidEnd != null) {
                int year = bidEnd.getYear();
                int currentYear = java.time.LocalDateTime.now().getYear();
                if (year < 2020 || year > currentYear) {
                    bidEnd = null;
                }
            }
            
            java.time.LocalDateTime bidStart = item.getBidStart();
            if (bidStart != null) {
                int year = bidStart.getYear();
                int currentYear = java.time.LocalDateTime.now().getYear();
                if (year < 2020 || year > currentYear) {
                    bidStart = null;
                }
            }
            
            map.put("endDate", bidEnd);
            map.put("bidEnd", bidEnd);
            map.put("startDate", bidStart);
            map.put("bidStart", bidStart);
            
            // 기타 정보
            map.put("orgName", item.getOrgName() != null ? item.getOrgName() : "");
            map.put("plnmNo", item.getPlnmNo());
            map.put("appraisalAmount", item.getAppraisalAmountMax() != null ? item.getAppraisalAmountMax() : 0L);
            map.put("minBidPrice", item.getMinBidPriceMin() != null ? item.getMinBidPriceMin() : 0L);
            map.put("assetCategory", item.getAssetCategory());
            
            atList.add(map);
        }
        
        return atList;
    }
    
    /**
     * 같은 물건 ID가 여러 번 나오면 최신 것만 표시하고 나머지는 유찰 횟수로 카운트
     * 같은 plnmNo 또는 cltrMnmtNo를 가진 물건들을 그룹화하여 처리
     */
    private List<ItemDetail> processDuplicateItems(List<ItemDetail> items) {
        if (items == null || items.isEmpty()) {
            log.warn("⚠️ processDuplicateItems: items가 null이거나 비어있음");
            return List.of();
        }
        
        log.info("🔄 processDuplicateItems 시작: 원본 아이템 {}개", items.size());
        
        try {
            // plnmNo 또는 cltrMnmtNo를 기준으로 그룹화
            Map<String, List<ItemDetail>> groupedByKey = new java.util.HashMap<>();
            int nullKeyCount = 0;
            int plnmKeyCount = 0;
            int cltrKeyCount = 0;
            
            for (ItemDetail item : items) {
                if (item == null) {
                    log.warn("⚠️ processDuplicateItems: null 아이템 발견, 건너뜀");
                    continue;
                }
                
                // 그룹 키 생성: plnmNo 우선, 없으면 cltrMnmtNo 사용
                String key = null;
                if (item.getPlnmNo() != null && !item.getPlnmNo().toString().trim().isEmpty()) {
                    key = "plnm_" + item.getPlnmNo();
                    plnmKeyCount++;
                } else if (item.getCltrMnmtNo() != null && !item.getCltrMnmtNo().trim().isEmpty()) {
                    key = "cltr_" + item.getCltrMnmtNo();
                    cltrKeyCount++;
                } else {
                    // 키가 없으면 그대로 추가 (중복 처리 불가)
                    key = "unique_" + System.identityHashCode(item);
                    nullKeyCount++;
                }
                
                groupedByKey.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(item);
            }
            
            log.info("🔄 그룹화 완료: plnmNo 키={}개, cltrMnmtNo 키={}개, 키 없음={}개, 총 그룹={}개", 
                plnmKeyCount, cltrKeyCount, nullKeyCount, groupedByKey.size());
            
            List<ItemDetail> result = new java.util.ArrayList<>();
            
            // 각 그룹에서 최신 것만 선택하고 유찰 횟수 계산
            for (List<ItemDetail> group : groupedByKey.values()) {
                if (group == null || group.isEmpty()) {
                    continue;
                }
                
                if (group.size() == 1) {
                    // 중복이 없으면 그대로 추가 (유찰 횟수는 0 또는 기존 값)
                    ItemDetail item = group.get(0);
                    if (item != null) {
                        if (item.getBidCount() == null) {
                            item.setBidCount(0);
                        }
                        log.debug("🔄 단일 물건: plnmNo={}, cltrMnmtNo={}, 유찰 횟수={}", 
                            item.getPlnmNo(), item.getCltrMnmtNo(), item.getBidCount());
                        result.add(item);
                    }
                } else {
                    // 같은 물건이 여러 개면 bidStart가 가장 최근인 것을 찾기
                    ItemDetail latest = group.stream()
                        .filter(item -> item != null && item.getBidStart() != null)
                        .max((a, b) -> a.getBidStart().compareTo(b.getBidStart()))
                        .orElse(group.stream()
                            .filter(item -> item != null)
                            .findFirst()
                            .orElse(null));
                    
                    if (latest == null) {
                        log.warn("⚠️ processDuplicateItems: 그룹에서 유효한 아이템을 찾을 수 없음");
                        continue;
                    }
                    
                    // 나머지 것들은 유찰 횟수로 카운트
                    int bidCount = group.size() - 1;  // 최신 것 제외한 나머지 개수
                    
                    // 기존 bidCount가 있으면 더하기
                    if (latest.getBidCount() != null && latest.getBidCount() > 0) {
                        bidCount += latest.getBidCount();
                    }
                    
                    latest.setBidCount(bidCount);
                    result.add(latest);
                    
                    log.info("🔄 중복 물건 처리: plnmNo={}, cltrMnmtNo={}, 총 {}개 중 최신 것 선택, 유찰 횟수={}", 
                        latest.getPlnmNo(), latest.getCltrMnmtNo(), group.size(), bidCount);
                }
            }
            
            // 유찰 횟수가 0보다 큰 아이템 개수 확인
            long itemsWithBidCount = result.stream()
                .filter(item -> item != null && item.getBidCount() != null && item.getBidCount() > 0)
                .count();
            
            // 그룹 크기 통계
            Map<Integer, Long> groupSizeStats = groupedByKey.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(List::size, java.util.stream.Collectors.counting()));
            
            log.info("✅ processDuplicateItems 완료: {}개 그룹 처리, {}개 결과 반환, 유찰 횟수 > 0인 아이템: {}개", 
                groupedByKey.size(), result.size(), itemsWithBidCount);
            log.info("📊 그룹 크기 통계: {}", groupSizeStats);
            
            // 큰 그룹(중복이 많은 경우) 샘플 로그
            groupedByKey.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 5)
                .limit(5)
                .forEach(entry -> {
                    ItemDetail sample = entry.getValue().get(0);
                    log.info("📊 큰 그룹 샘플: key={}, 그룹 크기={}, plnmNo={}, cltrMnmtNo={}", 
                        entry.getKey(), entry.getValue().size(), 
                        sample != null ? sample.getPlnmNo() : null,
                        sample != null ? sample.getCltrMnmtNo() : null);
                });
            
            // 유찰 횟수가 있는 아이템 샘플 로그
            result.stream()
                .filter(item -> item != null && item.getBidCount() != null && item.getBidCount() > 0)
                .limit(5)
                .forEach(item -> log.info("📊 유찰 횟수 샘플: plnmNo={}, cltrMnmtNo={}, bidCount={}", 
                    item.getPlnmNo(), item.getCltrMnmtNo(), item.getBidCount()));
            
            return result;
        } catch (Exception e) {
            log.error("❌ processDuplicateItems 처리 중 예외 발생: {}", e.getMessage(), e);
            // 예외 발생 시 원본 데이터 반환
            return items;
        }
    }
}
