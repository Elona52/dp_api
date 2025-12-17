package com.api.union.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * =================================================================== 
 * Onbid 외부 API 호출 서비스
 * =================================================================== 
 * 1.getUnifyUsageCltr: 용도별 통합 조회 (실제로는 신규물건과 유사한 데이터 반환)
 * 2.getUnifyNewCltrList: 신물건 조회 
 * 3.getUnifyDegression50PerCltrList: 감가 50% 이상 물건 조회
 * 
 * 참고: Onbid API에는 "전체 경매물건"을 조회하는 별도 API가 없을 수 있습니다.
 * getUnifyUsageCltr는 용도별로 필터링된 물건을 조회하는 API입니다.
 */

@Service
public class ApiService {

    private final RestTemplate restTemplate;

    @Value("${onbid.serviceKey}")
    private String serviceKey;

    public ApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

 // 신물건 조회
    public String getUnifyNewCltrList(int pageNo, int numOfRows, String sido) {
        String url = "http://openapi.onbid.co.kr/openapi/services/ThingInfoInquireSvc/getUnifyNewCltrList"
                + "?serviceKey=" + serviceKey
                + "&DPSL_MTD_CD=0001"  // 처분방식코드: 0001=매각
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&SIDO=" + sido;
        
        System.out.println("🟢 [신물건 조회 API] URL: " + url);
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("🟢 [신물건 조회 API] 응답 길이: " + (response != null ? response.length() : 0));
            return response;
        } catch (Exception e) {
            System.err.println("❌ [신물건 조회 API] 호출 실패: " + e.getMessage());
            throw new RuntimeException("신물건 조회 API 호출 실패: " + e.getMessage(), e);
        }
    }

    // 감가 50% 조회
    public String getUnifyDegression50PerCltrList(int pageNo, int numOfRows, String sido) {
        String url = "http://openapi.onbid.co.kr/openapi/services/ThingInfoInquireSvc/getUnifyDegression50PerCltrList"
                + "?serviceKey=" + serviceKey
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&SIDO=" + sido;
        return restTemplate.getForObject(url, String.class);
    }

    // 용도별 통합 조회 (전체 경매물건 조회용)
    // DPSL_MTD_CD=0001 (매각) 파라미터를 추가하여 전체 경매물건 조회
    public String getUnifyUsageCltrList(int pageNo, int numOfRows, String sido) {
        System.out.println("🔵 [용도별 통합 조회 API] 호출 시작: pageNo=" + pageNo + ", numOfRows=" + numOfRows + ", sido=" + sido);
        System.out.println("🔵 [용도별 통합 조회 API] serviceKey 설정 여부: " + (serviceKey != null && !serviceKey.isEmpty()));
        
        String url = "http://openapi.onbid.co.kr/openapi/services/ThingInfoInquireSvc/getUnifyUsageCltr"
                + "?serviceKey=" + serviceKey
                + "&DPSL_MTD_CD=0001"  // 처분방식코드: 0001=매각
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&SIDO=" + sido;
        
        System.out.println("🔵 [용도별 통합 조회 API - 전체 경매물건] URL: " + url);
        
        try {
            System.out.println("🔵 [용도별 통합 조회 API] RestTemplate 호출 시작...");
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("🔵 [용도별 통합 조회 API] RestTemplate 호출 완료");
            System.out.println("🔵 [용도별 통합 조회 API] 응답 != null: " + (response != null));
            
            if (response == null) {
                System.err.println("❌ [용도별 통합 조회 API] 응답이 null입니다!");
                return null;
            }
            
            System.out.println("🔵 [용도별 통합 조회 API] 응답 길이: " + response.length());
            
            // 응답 내용 확인 (에러 메시지 체크)
            if (response.contains("<resultCode>") || response.contains("<resultMsg>")) {
                System.err.println("❌ [용도별 통합 조회 API] API 에러 응답 감지!");
                if (response.length() < 1000) {
                    System.err.println("❌ [용도별 통합 조회 API] 에러 응답 내용: " + response);
                } else {
                    System.err.println("❌ [용도별 통합 조회 API] 에러 응답 일부: " + response.substring(0, 1000));
                }
            }
            
            if (response.length() > 0 && response.length() < 500) {
                System.out.println("🔵 [용도별 통합 조회 API] 응답 내용: " + response);
            } else if (response.length() > 0) {
                System.out.println("🔵 [용도별 통합 조회 API] 응답 시작 부분: " + response.substring(0, Math.min(200, response.length())));
            }
            
            return response;
        } catch (Exception e) {
            System.err.println("❌ [용도별 통합 조회 API] 호출 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("용도별 통합 조회 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
