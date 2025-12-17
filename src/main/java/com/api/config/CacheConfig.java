package com.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ===================================================================
 * 캐시 설정 클래스
 * ===================================================================
 * API 응답을 캐싱하여 성능 개선
 * - 동일한 요청에 대해 캐시된 결과를 즉시 반환하여 응답 시간 단축
 * - 주의: ConcurrentMapCacheManager는 기본적으로 TTL이 없지만,
 *   동일한 요청에 대해서는 즉시 캐시된 결과를 반환하므로 성능 개선 효과가 있음
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 메모리 기반 캐시 매니저 사용
        // 캐시 이름: "apiItems", "apiNewItems", "apiUsageItems"
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("apiItems", "apiNewItems", "apiUsageItems");
        cacheManager.setAllowNullValues(false); // null 값은 캐싱하지 않음
        return cacheManager;
    }
}
