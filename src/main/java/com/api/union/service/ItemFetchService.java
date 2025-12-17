package com.api.union.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.api.item.dto.ItemDetail;
import com.api.item.service.ItemRestService;
import com.api.util.ApiXmlParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemFetchService {

    private final ApiService apiService;
    private final ItemRestService itemService;

    private static final String DEFAULT_SIDO = "서울특별시"; // 시도 필터 기본값
    private static final int PAGE_SIZE = 200; // 페이지당 200건

    /** 신물건 전체 조회 후 DB 저장 */
    public int fetchAndSaveAllNewItems() {
        int pageNo = 1;
        int totalSaved = 0;
        int numOfRows = 100;

        while (true) {
            String xml = apiService.getUnifyNewCltrList(pageNo, numOfRows, DEFAULT_SIDO);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);

            if (details.isEmpty()) break;

            totalSaved += itemService.upsertItems(details);
            pageNo++;
        }

        return totalSaved;
    }

    /** 감가 50% 이상 전체 조회 후 DB 저장 */
    public int fetchAndSaveAllDiscountItems() {
        int pageNo = 1;
        int totalSaved = 0;
        int numOfRows = 100;

        while (true) {
            String xml = apiService.getUnifyDegression50PerCltrList(pageNo, numOfRows, DEFAULT_SIDO);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);

            if (details.isEmpty()) break;

            totalSaved += itemService.upsertItems(details);
            pageNo++;
        }

        return totalSaved;
    }

    /** 용도별 통합 조회 전체 후 DB 저장 */
    public int fetchAndSaveAllUsageItems() {
        int pageNo = 1;
        int totalSaved = 0;
        int numOfRows = 100;

        while (true) {
            String xml = apiService.getUnifyUsageCltrList(pageNo, numOfRows, DEFAULT_SIDO);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);

            if (details.isEmpty()) break;

            totalSaved += itemService.upsertItems(details);
            pageNo++;
        }

        return totalSaved;
    }

    // =============================================================================
    // 페이지네이션 기능 (200건씩)
    // =============================================================================

    /**
     * 신물건 조회 (페이지네이션) - 200건씩
     */
    public List<ItemDetail> fetchNewItems(int page, String sido) {
        try {
            String xml = apiService.getUnifyNewCltrList(page, PAGE_SIZE, sido);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("📡 신물건 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            return details != null ? details : List.of();
        } catch (Exception e) {
            log.error("❌ 신물건 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 감가 50% 이상 조회 (페이지네이션) - 200건씩
     */
    public List<ItemDetail> fetchDiscountItems(int page, String sido) {
        try {
            String xml = apiService.getUnifyDegression50PerCltrList(page, PAGE_SIZE, sido);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("📡 감가 50% 이상 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            return details != null ? details : List.of();
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 용도별 통합 조회 (페이지네이션) - 200건씩
     */
    public List<ItemDetail> fetchUsageItems(int page, String sido) {
        try {
            String xml = apiService.getUnifyUsageCltrList(page, PAGE_SIZE, sido);
            List<ItemDetail> details = ApiXmlParser.parseNewItemDetails(xml);
            log.info("📡 용도별 통합 조회 완료: page={}, sido={}, count={}", page, sido, details != null ? details.size() : 0);
            return details != null ? details : List.of();
        } catch (Exception e) {
            log.error("❌ 용도별 통합 조회 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 신물건 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveNewItems(int page, String sido) {
        try {
            List<ItemDetail> details = fetchNewItems(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = itemService.upsertItems(details);
            log.info("💾 신물건 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ 신물건 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 감가 50% 이상 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveDiscountItems(int page, String sido) {
        try {
            List<ItemDetail> details = fetchDiscountItems(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = itemService.upsertItems(details);
            log.info("💾 감가 50% 이상 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ 감가 50% 이상 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 용도별 통합 조회 후 DB 저장 (페이지네이션) - 200건씩
     */
    public int fetchAndSaveUsageItems(int page, String sido) {
        try {
            List<ItemDetail> details = fetchUsageItems(page, sido);
            if (details.isEmpty()) {
                return 0;
            }
            int saved = itemService.upsertItems(details);
            log.info("💾 용도별 통합 저장 완료: page={}, sido={}, saved={}", page, sido, saved);
            return saved;
        } catch (Exception e) {
            log.error("❌ 용도별 통합 저장 실패: page={}, sido={}, error={}", page, sido, e.getMessage(), e);
            return 0;
        }
    }
}
