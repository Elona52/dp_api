package com.api.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * ===================================================================
 * RestTemplate HTTP 클라이언트 설정
 * ===================================================================
 */
@Configuration  // Spring 설정 클래스로 등록
public class RestTemplateConfig {

    @SuppressWarnings("removal")
	@Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))  // 연결 타임아웃: 5초
                .setReadTimeout(Duration.ofSeconds(10))   // 읽기 타임아웃: 10초
                .build();
    }
}

