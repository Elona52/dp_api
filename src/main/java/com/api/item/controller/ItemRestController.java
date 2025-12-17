package com.api.item.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.admin.domain.ItemListResponse;
import com.api.admin.domain.OperationResponse;
import com.api.item.domain.Item;
import com.api.item.dto.ItemBasic;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemRestController {

    private final ItemRestService service;
    
    /** 목록 조회 (기본정보만) */
    @GetMapping
    public List<ItemBasic> getItemList() {
        return service.getItemList();
    }

    /** 상세 조회 (기본 + 상세 조인 정보) */
    @GetMapping("/{plnmNo}")
    public ItemDetail getItemDetail(@PathVariable Long plnmNo) {
        return service.getItemDetail(plnmNo);
    }

    /** 신규 물건 목록 */
    @GetMapping("/new")
    public List<ItemBasic> getNewItems() {
        return service.getNewItems();
    }

    /** 할인/급매/특가 등 목록 */
    @GetMapping("/discount")
    public List<ItemBasic> getDiscountItems() {
        return service.getDiscountItems();
    }

    // =============================================================================
    // DB 조회 (페이징)
    // =============================================================================

    /**
     * DB에서 신물건 조회 (페이징) - 14일 이내
     * GET /items/db/new-items?page=1&size=200
     */
    @GetMapping("/db/new-items")
    public ResponseEntity<ItemListResponse> getNewItemsFromDb(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "200") int size) {
        
        log.info("📊 DB 신물건 조회: page={}, size={}", page, size);
        
        try {
            List<ItemDetail> itemDetails = service.getNewItemsDetail(page, size);
            int totalCount = service.countNewItems();
            List<Item> items = ItemDetail.toItems(itemDetails);
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("DB")
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .currentPageCount(items != null ? items.size() : 0)
                    .items(items != null ? items : List.of())
                    .message("신물건 DB 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ 신물건 DB 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ItemListResponse.builder()
                            .success(false)
                            .message("신물건 DB 조회 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    /**
     * DB에서 감가 50% 이상 물건 조회 (페이징)
     * GET /items/db/discount-items?page=1&size=200
     */
    @GetMapping("/db/discount-items")
    public ResponseEntity<ItemListResponse> getDiscountItemsFromDb(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "200") int size) {
        
        log.info("📊 DB 감가 50% 이상 물건 조회: page={}, size={}", page, size);
        
        try {
            List<ItemDetail> itemDetails = service.getDiscountItemsDetail(page, size);
            int totalCount = service.countDiscountItems();
            List<Item> items = ItemDetail.toItems(itemDetails);
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("DB")
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .currentPageCount(items != null ? items.size() : 0)
                    .items(items != null ? items : List.of())
                    .message("감가 50% 이상 물건 DB 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 물건 DB 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ItemListResponse.builder()
                            .success(false)
                            .message("감가 50% 이상 물건 DB 조회 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    /**
     * DB에서 용도별 통합 물건 조회 (페이징) - 서울특별시
     * GET /items/db/usage-items?page=1&size=200
     */
    @GetMapping("/db/usage-items")
    public ResponseEntity<ItemListResponse> getUsageItemsFromDb(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "200") int size) {
        
        log.info("📊 DB 용도별 통합 물건 조회: page={}, size={}", page, size);
        
        try {
            List<ItemDetail> itemDetails = service.getItemsSeoul(page, size);
            int totalCount = service.countItemsSeoul();
            List<Item> items = ItemDetail.toItems(itemDetails);
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("DB")
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .currentPageCount(items != null ? items.size() : 0)
                    .items(items != null ? items : List.of())
                    .message("용도별 통합 물건 DB 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ 용도별 통합 물건 DB 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ItemListResponse.builder()
                            .success(false)
                            .message("용도별 통합 물건 DB 조회 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    // =============================================================================
    // DB 삭제
    // =============================================================================

    /**
     * 신물건 DB 삭제 (14일 이내 데이터)
     * DELETE /items/db/delete-new-items
     */
    @DeleteMapping("/db/delete-new-items")
    public ResponseEntity<OperationResponse> deleteNewItems() {
        log.info("🗑️ 신물건 DB 삭제 요청");
        
        try {
            int deleted = service.deleteNewItems();
            return ResponseEntity.ok(OperationResponse.builder()
                    .success(true)
                    .message("신물건 삭제 완료: " + deleted + "건")
                    .affectedRows(deleted)
                    .build());
        } catch (Exception e) {
            log.error("❌ 신물건 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OperationResponse.builder()
                            .success(false)
                            .message("신물건 삭제 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    /**
     * 감가 50% 이상 물건 DB 삭제
     * DELETE /items/db/delete-discount-items
     */
    @DeleteMapping("/db/delete-discount-items")
    public ResponseEntity<OperationResponse> deleteDiscountItems() {
        log.info("🗑️ 감가 50% 이상 물건 DB 삭제 요청");
        
        try {
            int deleted = service.deleteDiscountItems();
            return ResponseEntity.ok(OperationResponse.builder()
                    .success(true)
                    .message("감가 50% 이상 물건 삭제 완료: " + deleted + "건")
                    .affectedRows(deleted)
                    .build());
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 물건 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OperationResponse.builder()
                            .success(false)
                            .message("감가 50% 이상 물건 삭제 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    /**
     * 용도별 통합 물건 DB 삭제 (서울특별시 전체)
     * DELETE /items/db/delete-usage-items
     */
    @DeleteMapping("/db/delete-usage-items")
    public ResponseEntity<OperationResponse> deleteUsageItems() {
        log.info("🗑️ 용도별 통합 물건 DB 삭제 요청");
        
        try {
            int deleted = service.deleteUsageItems();
            return ResponseEntity.ok(OperationResponse.builder()
                    .success(true)
                    .message("용도별 통합 물건 삭제 완료: " + deleted + "건")
                    .affectedRows(deleted)
                    .build());
        } catch (Exception e) {
            log.error("❌ 용도별 통합 물건 삭제 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OperationResponse.builder()
                            .success(false)
                            .message("용도별 통합 물건 삭제 실패: " + e.getMessage())
                            .errorType(e.getClass().getSimpleName())
                            .build());
        }
    }

    // =============================================================================
    // ApiService를 통한 일괄 저장 (여러 페이지 순회)
    // =============================================================================

    /**
     * ApiService를 통한 신물건 일괄 저장 (여러 페이지 순회)
     * POST /items/api/batch-save-new-items
     * Body: { "sido": "서울특별시" }
     */
    @PostMapping("/api/batch-save-new-items")
    public ResponseEntity<Map<String, Object>> batchSaveNewItems(@RequestBody Map<String, Object> request) {
        String sido = (String) request.getOrDefault("sido", "서울특별시");
        log.info("💾 ApiService 신물건 일괄 저장 요청: sido={}", sido);
        
        try {
            int totalSaved = 0;
            int maxPages = 50;
            
            for (int page = 1; page <= maxPages; page++) {
                int saved = service.fetchAndSaveNewItemsFromApi(page, sido);
                if (saved == 0) {
                    log.info("💾 ApiService 신물건 저장 종료: page={}에서 데이터 없음", page);
                    break;
                }
                totalSaved += saved;
                log.info("💾 ApiService 신물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);
                
                if (saved < 200) {
                    log.info("💾 ApiService 신물건 저장 종료: page={}에서 200건 미만", page);
                    break;
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("💾 ApiService 신물건 일괄 저장 완료: 총 {}건", totalSaved);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ApiService 신물건 일괄 저장 완료: " + totalSaved + "건",
                    "savedCount", totalSaved,
                    "totalRequested", totalSaved
            ));
        } catch (Exception e) {
            log.error("❌ ApiService 신물건 일괄 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "ApiService 신물건 일괄 저장 실패: " + e.getMessage(),
                            "errorType", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * ApiService를 통한 감가 50% 이상 물건 일괄 저장 (여러 페이지 순회)
     * POST /items/api/batch-save-discount-items
     * Body: { "sido": "서울특별시" }
     */
    @PostMapping("/api/batch-save-discount-items")
    public ResponseEntity<Map<String, Object>> batchSaveDiscountItems(@RequestBody Map<String, Object> request) {
        String sido = (String) request.getOrDefault("sido", "서울특별시");
        log.info("💾 ApiService 감가 50% 이상 물건 일괄 저장 요청: sido={}", sido);
        
        try {
            int totalSaved = 0;
            int maxPages = 50;
            
            for (int page = 1; page <= maxPages; page++) {
                int saved = service.fetchAndSaveDiscountItemsFromApi(page, sido);
                if (saved == 0) {
                    log.info("💾 ApiService 감가 50% 이상 물건 저장 종료: page={}에서 데이터 없음", page);
                    break;
                }
                totalSaved += saved;
                log.info("💾 ApiService 감가 50% 이상 물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);
                
                if (saved < 200) {
                    log.info("💾 ApiService 감가 50% 이상 물건 저장 종료: page={}에서 200건 미만", page);
                    break;
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("💾 ApiService 감가 50% 이상 물건 일괄 저장 완료: 총 {}건", totalSaved);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ApiService 감가 50% 이상 물건 일괄 저장 완료: " + totalSaved + "건",
                    "savedCount", totalSaved,
                    "totalRequested", totalSaved
            ));
        } catch (Exception e) {
            log.error("❌ ApiService 감가 50% 이상 물건 일괄 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "ApiService 감가 50% 이상 물건 일괄 저장 실패: " + e.getMessage(),
                            "errorType", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * ApiService를 통한 용도별 통합 물건 일괄 저장 (여러 페이지 순회)
     * POST /items/api/batch-save-usage-items
     * Body: { "sido": "서울특별시" }
     */
    @PostMapping("/api/batch-save-usage-items")
    public ResponseEntity<Map<String, Object>> batchSaveUsageItems(@RequestBody Map<String, Object> request) {
        String sido = (String) request.getOrDefault("sido", "서울특별시");
        log.info("💾 ApiService 용도별 통합 물건 일괄 저장 요청: sido={}", sido);
        
        try {
            int totalSaved = 0;
            int maxPages = 50;
            
            for (int page = 1; page <= maxPages; page++) {
                int saved = service.fetchAndSaveUsageItemsFromApi(page, sido);
                if (saved == 0) {
                    log.info("💾 ApiService 용도별 통합 물건 저장 종료: page={}에서 데이터 없음", page);
                    break;
                }
                totalSaved += saved;
                log.info("💾 ApiService 용도별 통합 물건 저장 진행: page={}, saved={}, total={}", page, saved, totalSaved);
                
                if (saved < 200) {
                    log.info("💾 ApiService 용도별 통합 물건 저장 종료: page={}에서 200건 미만", page);
                    break;
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            log.info("💾 ApiService 용도별 통합 물건 일괄 저장 완료: 총 {}건", totalSaved);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ApiService 용도별 통합 물건 일괄 저장 완료: " + totalSaved + "건",
                    "savedCount", totalSaved,
                    "totalRequested", totalSaved
            ));
        } catch (Exception e) {
            log.error("❌ ApiService 용도별 통합 물건 일괄 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "ApiService 용도별 통합 물건 일괄 저장 실패: " + e.getMessage(),
                            "errorType", e.getClass().getSimpleName()
                    ));
        }
    }
    
    // =============================================================================
    // 비동기 API 조회 (AJAX용)
    // =============================================================================
    
    /**
     * 전체 경매물건 조회 (AJAX용) - 비동기 로딩
     * GET /items/api/all-items?page=1&sido=서울특별시&category=주거용건물&pageSize=20
     */
    @GetMapping("/api/all-items")
    public ResponseEntity<Map<String, Object>> getAllItemsAsync(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        
        log.info("🔵 [비동기] 전체 경매물건 조회: page={}, sido={}, category={}, pageSize={}", page, sido, category, pageSize);
        
        try {
            log.info("🔵 [비동기] 전체 경매물건 조회 시작: page={}, sido={}, category={}, pageSize={}", page, sido, category, pageSize);
            
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * pageSize / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            log.info("🔵 [비동기] API 페이지 계산: apiPage={}", apiPage);
            
            List<ItemDetail> itemDetails = service.fetchAllItemsFromApi(apiPage, sido);
            log.info("🔵 [비동기] API 호출 완료: itemDetails.size()={}", itemDetails != null ? itemDetails.size() : 0);
            
            if (itemDetails == null || itemDetails.isEmpty()) {
                log.warn("⚠️ [비동기] API 응답이 비어있음");
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "atList", List.of(),
                        "totalCount", 0,
                        "pageNum", page,
                        "pageSize", pageSize,
                        "pageCount", 0,
                        "category", category != null ? category : "all",
                        "sido", sido,
                        "message", "조회된 데이터가 없습니다."
                ));
            }
            
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
                log.info("🔵 [비동기] 카테고리 필터링: {}개 -> {}개", beforeSize, itemDetails.size());
            }
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * pageSize) % 200;
            int endIndex = Math.min(startIndex + pageSize, itemDetails.size());
            List<ItemDetail> pagedItems;
            if (startIndex < itemDetails.size()) {
                pagedItems = itemDetails.subList(startIndex, endIndex);
            } else {
                pagedItems = new java.util.ArrayList<>();
            }
            
            log.info("🔵 [비동기] 페이지네이션 처리: startIndex={}, endIndex={}, pagedItems.size()={}", startIndex, endIndex, pagedItems.size());
            
            // 템플릿용 리스트 변환
            List<Map<String, Object>> atList = service.convertToAtList(pagedItems);
            log.info("🔵 [비동기] convertToAtList 완료: atList.size()={}", atList != null ? atList.size() : 0);
            
            // 총 개수는 추정값
            int totalCount = pagedItems.size() == pageSize ? (apiPage * 200) : ((apiPage - 1) * 200 + pagedItems.size());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("atList", atList != null ? atList : List.of());
            response.put("items", atList != null ? atList : List.of()); // 호환성을 위해 items도 추가
            response.put("totalCount", totalCount);
            response.put("pageNum", page);
            response.put("page", page); // 호환성
            response.put("size", pageSize); // 호환성
            response.put("pageSize", pageSize);
            response.put("pageCount", (int) Math.ceil((double) totalCount / pageSize));
            response.put("category", category != null ? category : "all");
            response.put("sido", sido);
            
            log.info("🔵 [비동기] 최종 응답 생성: success={}, atList.size()={}, totalCount={}", 
                response.get("success"), 
                atList != null ? atList.size() : 0, 
                totalCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [비동기] 전체 경매물건 조회 실패", e);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "전체 경매물건 조회 실패: " + e.getMessage(),
                            "atList", List.of(),
                            "totalCount", 0
                    ));
        }
    }
    
    /**
     * 신규물건 조회 (AJAX용) - 비동기 로딩
     * GET /items/api/new-items?page=1&sido=서울특별시&pageSize=20
     */
    @GetMapping("/api/new-items")
    public ResponseEntity<Map<String, Object>> getNewItemsAsync(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        
        log.info("🟢 [비동기] 신규물건 조회: page={}, sido={}, pageSize={}", page, sido, pageSize);
        
        try {
            // API는 200건씩 반환하므로, page에 맞는 API 페이지 계산
            int apiPage = (int) Math.ceil((double) (page - 1) * pageSize / 200.0) + 1;
            if (apiPage < 1) apiPage = 1;
            
            List<ItemDetail> itemDetails = service.fetchNewItemsFromApi(apiPage, sido);
            
            // 페이지네이션 처리
            int startIndex = ((page - 1) * pageSize) % 200;
            int endIndex = Math.min(startIndex + pageSize, itemDetails.size());
            if (startIndex < itemDetails.size()) {
                itemDetails = itemDetails.subList(startIndex, endIndex);
            } else {
                itemDetails = new java.util.ArrayList<>();
            }
            
            // 템플릿용 리스트 변환
            List<Map<String, Object>> atList = service.convertToAtList(itemDetails);
            
            // 총 개수는 추정값
            int totalCount = itemDetails.size() == pageSize ? (apiPage * 200) : ((apiPage - 1) * 200 + itemDetails.size());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "atList", atList != null ? atList : List.of(),
                    "totalCount", totalCount,
                    "pageNum", page,
                    "pageSize", pageSize,
                    "pageCount", (int) Math.ceil((double) totalCount / pageSize),
                    "category", "신규물건",
                    "sido", sido
            ));
        } catch (Exception e) {
            log.error("❌ [비동기] 신규물건 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "신규물건 조회 실패: " + e.getMessage(),
                            "atList", List.of(),
                            "totalCount", 0
                    ));
        }
    }
}
