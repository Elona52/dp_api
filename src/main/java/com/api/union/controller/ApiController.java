package com.api.union.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.admin.domain.ItemListResponse;
import com.api.item.domain.Item;
import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.union.service.ItemFetchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final ItemFetchService itemFetchService; // DB 저장용
    private final ItemRestService itemRestService; // 조회용
     
    @PostMapping("/all-new-items/save")
    public String saveAllNewItems() {
        int saved = itemFetchService.fetchAndSaveAllNewItems();
        return saved + "건 저장 완료";
    }

    @PostMapping("/all-discount-items/save")
    public String saveAllDiscountItems() {
        int saved = itemFetchService.fetchAndSaveAllDiscountItems();
        return saved + "건 저장 완료";
    }

    @PostMapping("/all-usage-items/save")
    public String saveAllUsageItems() {
        int saved = itemFetchService.fetchAndSaveAllUsageItems();
        return saved + "건 저장 완료";
    }

    // =============================================================================
    // 페이지네이션 기능 (200건씩)
    // =============================================================================

    /**
     * 신물건 조회 (페이지네이션) - 200건씩
     * GET /api/union/new-items?page=1&sido=서울특별시
     */
    @GetMapping("/union/new-items")
    public ResponseEntity<ItemListResponse> getNewItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📡 Union 신물건 조회: page={}, sido={}", page, sido);
        
        try {
            List<ItemDetail> details = itemRestService.fetchNewItemsFromApi(page, sido);
            List<Item> items = ItemDetail.toItems(details);
            
            // 다음 페이지가 있는지 확인 (현재 페이지의 아이템 개수가 200과 같으면 다음 페이지가 있을 가능성)
            boolean hasNextPage = details.size() == 200;
            int estimatedTotal = hasNextPage ? (page * 200) + 200 : (page - 1) * 200 + details.size();
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("UNION_API")
                    .page(page)
                    .size(200)
                    .sido(sido)
                    .totalCount(estimatedTotal)
                    .currentPageCount(details.size())
                    .items(items)
                    .message("신물건 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ Union 신물건 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(false)
                    .message("신물건 조회 실패: " + e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .build());
        }
    }

    /**
     * 감가 50% 이상 조회 (페이지네이션) - 200건씩
     * GET /api/union/discount-items?page=1&sido=서울특별시
     */
    @GetMapping("/union/discount-items")
    public ResponseEntity<ItemListResponse> getDiscountItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📡 Union 감가 50% 이상 조회: page={}, sido={}", page, sido);
        
        try {
            List<ItemDetail> details = itemRestService.fetchDiscountItemsFromApi(page, sido);
            List<Item> items = ItemDetail.toItems(details);
            
            boolean hasNextPage = details.size() == 200;
            int estimatedTotal = hasNextPage ? (page * 200) + 200 : (page - 1) * 200 + details.size();
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("UNION_API")
                    .page(page)
                    .size(200)
                    .sido(sido)
                    .totalCount(estimatedTotal)
                    .currentPageCount(details.size())
                    .items(items)
                    .message("감가 50% 이상 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ Union 감가 50% 이상 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(false)
                    .message("감가 50% 이상 조회 실패: " + e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .build());
        }
    }

    /**
     * 용도별 통합 조회 (페이지네이션) - 200건씩
     * GET /api/union/usage-items?page=1&sido=서울특별시
     */
    @GetMapping("/union/usage-items")
    public ResponseEntity<ItemListResponse> getUsageItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📡 Union 용도별 통합 조회: page={}, sido={}", page, sido);
        
        try {
            List<ItemDetail> details = itemRestService.fetchUsageItemsFromApi(page, sido);
            List<Item> items = ItemDetail.toItems(details);
            
            boolean hasNextPage = details.size() == 200;
            int estimatedTotal = hasNextPage ? (page * 200) + 200 : (page - 1) * 200 + details.size();
            
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(true)
                    .source("UNION_API")
                    .page(page)
                    .size(200)
                    .sido(sido)
                    .totalCount(estimatedTotal)
                    .currentPageCount(details.size())
                    .items(items)
                    .message("용도별 통합 조회 성공")
                    .build());
        } catch (Exception e) {
            log.error("❌ Union 용도별 통합 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(ItemListResponse.builder()
                    .success(false)
                    .message("용도별 통합 조회 실패: " + e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .build());
        }
    }

    /**
     * 신물건 조회 후 DB 저장 (페이지네이션) - 200건씩
     * POST /api/union/new-items/save?page=1&sido=서울특별시
     */
    @PostMapping("/union/new-items/save")
    public ResponseEntity<String> saveNewItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📥 신물건 저장 요청 수신: page={}, sido={}", page, sido);
        
        try {
            int saved = itemFetchService.fetchAndSaveNewItems(page, sido);
            return ResponseEntity.ok(saved + "건 저장 완료 (페이지 " + page + ")");
        } catch (Exception e) {
            log.error("❌ 신물건 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok("저장 실패: " + e.getMessage());
        }
    }

    /**
     * 감가 50% 이상 조회 후 DB 저장 (페이지네이션) - 200건씩
     * POST /api/union/discount-items/save?page=1&sido=서울특별시
     */
    @PostMapping("/union/discount-items/save")
    public ResponseEntity<String> saveDiscountItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📥 감가 50% 이상 저장 요청 수신: page={}, sido={}", page, sido);
        
        try {
            int saved = itemFetchService.fetchAndSaveDiscountItems(page, sido);
            return ResponseEntity.ok(saved + "건 저장 완료 (페이지 " + page + ")");
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok("저장 실패: " + e.getMessage());
        }
    }

    /**
     * 용도별 통합 조회 후 DB 저장 (페이지네이션) - 200건씩
     * POST /api/union/usage-items/save?page=1&sido=서울특별시
     */
    @PostMapping("/union/usage-items/save")
    public ResponseEntity<String> saveUsageItems(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sido", defaultValue = "서울특별시") String sido) {
        
        log.info("📥 용도별 통합 저장 요청 수신: page={}, sido={}", page, sido);
        
        try {
            int saved = itemFetchService.fetchAndSaveUsageItems(page, sido);
            return ResponseEntity.ok(saved + "건 저장 완료 (페이지 " + page + ")");
        } catch (Exception e) {
            log.error("❌ 용도별 통합 저장 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok("저장 실패: " + e.getMessage());
        }
    }
}
