# 공공 부동산 경매 플랫폼 (ApiProj)

공공 부동산 경매 정보를 통합 관리하고 사용자가 입찰 및 결제를 진행할 수 있는 Spring Boot 기반의 웹 애플리케이션입니다.

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [주요 기능 및 작동 방식](#주요-기능-및-작동-방식)
- [화면과 백엔드 연결 구조](#화면과-백엔드-연결-구조)
- [데이터 흐름](#데이터-흐름)
- [설치 및 실행](#설치-및-실행)

---

## 프로젝트 개요

이 프로젝트는 **온비드(Onbid) 공공 부동산 경매 API**를 연동하여 실시간 경매 정보를 수집하고, 사용자에게 직관적인 인터페이스를 제공하는 웹 애플리케이션입니다.

### 주요 목적
- 공공 부동산 경매 물건 정보의 통합 조회 및 관리
- 사용자 친화적인 경매 물건 검색 및 필터링
- 입찰 신청 및 결제 시스템 구축
- 관심 물건 관리 및 가격 알림 기능 제공

---

## 기술 스택

### 백엔드
- **Spring Boot 3.5.9-SNAPSHOT** - 메인 프레임워크
- **Java 21** - 프로그래밍 언어
- **Spring MVC** - 웹 애플리케이션 계층
- **Spring Security** - 인증 및 보안
- **MyBatis 3.0.5** - ORM 프레임워크
- **Thymeleaf** - 서버 사이드 템플릿 엔진

### 데이터베이스
- **MariaDB** - 주 데이터베이스

### 외부 API 연동
- **온비드(Onbid) API** - 공공 부동산 경매 정보 수집
- **아임포트(Iamport)** - 결제 시스템 연동

### 프론트엔드
- **Thymeleaf** - 서버 사이드 렌더링
- **jQuery** - 클라이언트 사이드 스크립팅
- **Bootstrap 4** - UI 프레임워크

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        클라이언트 (브라우저)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  HTML/Thymeleaf │  │   JavaScript  │  │   CSS/Bootstrap │  │
│  └──────────────┘  ┌──────────────┘  └──────────────┘      │
└────────────────────┼─────────────────────────────────────────┘
                     │ HTTP Request/Response
┌────────────────────┼─────────────────────────────────────────┐
│              Spring Boot 애플리케이션                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Controller Layer                         │   │
│  │  ItemController │ MemberController │ PaymentController│   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Service Layer                            │   │
│  │  ItemService │ MemberService │ PaymentService       │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Mapper Layer (MyBatis)                   │   │
│  │  ItemMapper │ MemberMapper │ PaymentMapper          │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────┼─────────────────────────────────────────┘
                     │
┌────────────────────┼─────────────────────────────────────────┐
│              데이터베이스 (MariaDB)                            │
│  item_basic │ item_detail │ member │ payment_base │ ...      │
└────────────────────┼─────────────────────────────────────────┘
                     │
┌────────────────────┼─────────────────────────────────────────┐
│              외부 API                                          │
│  ┌──────────────┐              ┌──────────────┐            │
│  │  온비드 API   │              │  아임포트 API  │            │
│  └──────────────┘              └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

---

## 주요 기능 및 작동 방식

### 1. 메인 페이지 (`/`, `/main`)

#### 작동 흐름
1. **사용자 요청**: 브라우저에서 `/` 또는 `/main` 접근
2. **컨트롤러 처리**: `ItemController.mainPage()` 메서드 실행
3. **서비스 호출**: `ItemService`를 통해 데이터 조회
   - 신규물건 공지: `getMainPageNotices(5)` - 최근 5개 신규물건 조회
   - 카테고리 통계: `getMainPageCategoryStats()` - 용도별 물건 개수 통계
   - 금주 경매일정: `getMainPageScheduleList(10)` - 오늘 마감하는 경매 10개
   - 50% 체감 물건: `getMainPageDiscountList(4)` - 할인율 높은 물건 4개
4. **데이터 가공**: API에서 받은 데이터를 화면용 DTO로 변환
5. **템플릿 렌더링**: `main.html` 템플릿에 데이터 전달하여 HTML 생성
6. **응답 반환**: 완성된 HTML을 브라우저에 전송

#### 코드 예시
```java
@GetMapping({"/", "/main"})
public String mainPage(Model model) {
    // 1. 신규물건 공지 조회
    List<NoticeItem> notices = itemViewService.getMainPageNotices(5);
    model.addAttribute("notices", notices);
    
    // 2. 카테고리 통계 조회
    Map<String, Integer> categoryStatsMap = itemViewService.getMainPageCategoryStats();
    model.addAttribute("categoryStats", categoryStats);
    
    // 3. 경매일정 조회
    List<ScheduleItem> scheduleList = itemViewService.getMainPageScheduleList(10);
    model.addAttribute("scheduleList", scheduleList);
    
    // 4. 50% 체감 물건 조회
    List<DiscountItem> discountList = itemViewService.getMainPageDiscountList(4);
    model.addAttribute("discountList", discountList);
    
    return "main";  // templates/main.html 렌더링
}
```

---

### 2. 경매 물건 목록 페이지 (`/auctionList`)

#### 작동 흐름
1. **사용자 요청**: 브라우저에서 `/auctionList?category=주거용건물&sido=서울특별시&pageNum=1` 접근
2. **컨트롤러 처리**: `ItemController.auctionListPage()` 메서드 실행
3. **초기 데이터 로드**: 서버 사이드에서 첫 페이지 데이터 로드
   - `ItemRestService.fetchAllItemsFromApi()` 호출하여 온비드 API에서 데이터 가져오기
   - 카테고리 필터링 적용
   - 페이지네이션 처리
4. **템플릿 렌더링**: `item/list.html` 템플릿에 초기 데이터 전달
5. **비동기 로딩**: JavaScript에서 추가 페이지 데이터를 AJAX로 로드
   - `loadItemsAsync()` 함수가 `/items/api/all-items` 엔드포인트 호출
   - 응답 받은 데이터를 동적으로 테이블에 추가

#### 서버 사이드 + 클라이언트 사이드 하이브리드 방식
- **서버 사이드**: 초기 페이지 로드 시 첫 페이지 데이터를 서버에서 렌더링
- **클라이언트 사이드**: 페이지네이션, 필터링 변경 시 AJAX로 데이터 로드

#### 코드 예시
```java
@GetMapping("/auctionList")
public String auctionListPage(
        @RequestParam(name = "category", required = false) String category,
        @RequestParam(name = "sido", required = false) String sido,
        @RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
        Model model) {
    
    // 서버 사이드에서 초기 데이터 로드
    List<ItemDetail> itemDetails = itemRestService.fetchAllItemsFromApi(apiPage, sido);
    
    // 카테고리 필터링
    if (category != null) {
        itemDetails = itemDetails.stream()
            .filter(item -> item.getAssetCategory().contains(category))
            .collect(Collectors.toList());
    }
    
    // 페이지네이션 처리
    List<ItemDetail> pagedItems = itemDetails.subList(startIndex, endIndex);
    
    // 템플릿용 데이터 변환
    List<Map<String, Object>> atList = convertToAtList(pagedItems);
    model.addAttribute("atList", atList);
    model.addAttribute("asyncLoad", true);  // 비동기 로딩 플래그
    
    return "item/list";
}
```

#### JavaScript 비동기 로딩
```javascript
// list.html 내부의 JavaScript
function loadItemsAsync() {
    const apiUrl = '/items/api/all-items';
    const params = {
        page: pageNum,
        sido: sido,
        category: category,
        pageSize: pageSize
    };
    
    $.ajax({
        url: apiUrl,
        method: 'GET',
        data: params,
        success: function(response) {
            // 응답 데이터를 테이블에 동적으로 추가
            renderItems(response.atList);
        }
    });
}
```

---

### 3. 물건 상세 페이지 (`/api-item-detail`)

#### 작동 흐름
1. **사용자 요청**: 목록에서 물건 클릭 시 `/api-item-detail?itemId=123` 또는 `/api-item-detail?cltrNo=ABC123` 접근
2. **컨트롤러 처리**: `ItemController.apiItemDetailPage()` 메서드 실행
3. **물건 정보 조회**:
   - `itemId`가 있으면: `ItemService.getItemDetailById(itemId)` 호출
   - `cltrNo`가 있으면: `ItemService.getItemDetailByCltrMnmtNo(cltrNo)` 호출
   - DB에 없으면 온비드 API에서 직접 조회
4. **관련 정보 조회**: 입찰 이력, 관심목록 여부 등 추가 정보 조회
5. **템플릿 렌더링**: `item/api-detail.html` 템플릿에 데이터 전달

---

### 4. 회원 관리

#### 4-1. 회원가입 (`/memberJoin`)

**작동 흐름**:
1. **GET 요청**: `/memberJoin` 접근 → `member/join.html` 페이지 표시
2. **POST 요청**: 사용자가 폼 작성 후 제출
3. **컨트롤러 처리**: `MemberController.joinSubmit()` 실행
4. **서비스 처리**: `MemberService.join()` 실행
   - 아이디 중복 체크
   - 비밀번호 암호화 (BCrypt)
   - DB에 회원 정보 저장 (`member` 테이블)
5. **결과 처리**: 성공 시 `/main`으로 리다이렉트, 실패 시 에러 메시지와 함께 `member/join.html` 재표시

#### 4-2. 로그인 (`/login`)

**작동 흐름**:
1. **GET 요청**: `/memberLogin` 접근 → `member/login.html` 페이지 표시
2. **POST 요청**: 사용자가 아이디/비밀번호 입력 후 제출
3. **컨트롤러 처리**: `MemberController.login()` 실행
4. **서비스 처리**: `MemberService.login()` 실행
   - DB에서 회원 정보 조회
   - 비밀번호 검증 (BCrypt)
   - 세션에 로그인 정보 저장:
     ```java
     session.setAttribute("isLogin", true);
     session.setAttribute("loginId", member.getId());
     session.setAttribute("type", member.getType());
     ```
5. **Spring Security 연동**: `SessionAuthenticationFilter`가 세션 정보를 확인하여 Spring Security 인증 객체 생성
6. **결과 처리**: 성공 시 `/main`으로 리다이렉트

#### 4-3. 로그아웃 (`/logout`)

**작동 흐름**:
1. **GET 요청**: `/logout` 접근
2. **컨트롤러 처리**: `MemberController.logout()` 실행
3. **세션 무효화**: `session.invalidate()` 호출
4. **리다이렉트**: `/main`으로 이동

---

### 5. 입찰 및 결제 시스템

#### 5-1. 입찰서 작성 (`/payment/bid-form`)

**작동 흐름**:
1. **사용자 요청**: 물건 상세 페이지에서 "입찰하기" 버튼 클릭
2. **컨트롤러 처리**: `PaymentController.bidFormPage()` 실행
   - 로그인 여부 확인 (세션의 `loginId` 확인)
   - 물건 정보 조회 (`itemId` 또는 `cltrNo`로)
   - 입찰서 작성 페이지 데이터 준비
3. **템플릿 렌더링**: `payment/bid-form.html` 표시
4. **사용자 입력**: 입찰금액, 보증금, 입찰방식, 결제방법, 환불계좌 등 입력
5. **입찰서 제출**: JavaScript `submitBidForm()` 함수 실행
   - AJAX로 `/payment/submit-bid` POST 요청
   - `PaymentDetailService.handleSubmitBidRequest()` 실행
   - `payment_base` 테이블에 입찰 기본 정보 저장
   - `payment_detail` 테이블에 입찰 상세 정보 저장
   - `payment_status`는 "ready" 상태로 설정
6. **결과 처리**: 성공 시 `/payment/bid-submitted?paymentId=123`로 이동

#### 5-2. 결제 처리 (`/payment/checkout`)

**작동 흐름**:
1. **사용자 요청**: 입찰서 제출 후 결제 페이지로 이동
2. **컨트롤러 처리**: `PaymentController.checkoutPage()` 실행
   - `paymentId`로 입찰 정보 조회
   - 아임포트 결제 정보 준비
3. **템플릿 렌더링**: `payment/payment.html` 표시 (아임포트 JavaScript SDK 포함)
4. **결제 준비**: JavaScript에서 `/payment/prepare` POST 요청
   - 주문번호(`merchant_uid`) 생성
   - 아임포트에 결제 준비 요청
5. **결제 요청**: 아임포트 결제창 표시
6. **결제 완료**: 아임포트에서 `/payment/complete` POST 요청 (콜백)
   - 결제 검증 (아임포트 API로 실제 결제 확인)
   - `payment_base` 테이블의 `payment_status`를 "paid"로 업데이트
   - `payment_history` 테이블에 결제 이력 저장
7. **결과 처리**: 성공 시 `/payment/success`, 실패 시 `/payment/fail`로 리다이렉트

#### 결제 플로우 다이어그램
```
[입찰서 작성] 
    ↓
[입찰서 제출] → payment_base, payment_detail 테이블에 저장 (status: "ready")
    ↓
[결제 페이지] → 아임포트 결제창 표시
    ↓
[결제 요청] → 아임포트 API 호출
    ↓
[결제 완료 콜백] → /payment/complete
    ↓
[결제 검증] → 아임포트 API로 실제 결제 확인
    ↓
[DB 업데이트] → payment_status: "paid", payment_history 저장
    ↓
[결제 성공 페이지]
```

---

### 6. 관심목록 기능 (`/myFavorites`)

#### 작동 흐름
1. **관심목록 추가**: 물건 상세 페이지에서 "관심목록 추가" 버튼 클릭
   - JavaScript에서 `/favorite/add` POST 요청
   - `FavoriteService.addFavorite()` 실행
   - `favorite` 테이블에 `member_id`와 `plnm_no` 저장
2. **관심목록 조회**: `/myFavorites` 접근
   - `FavoriteController.myFavoritesPage()` 실행
   - 로그인한 사용자의 관심목록 조회
   - `favorite` 테이블과 `item_basic` 테이블 JOIN하여 물건 정보 조회
   - `member/favorites.html` 템플릿에 데이터 전달
3. **관심목록 삭제**: "삭제" 버튼 클릭
   - JavaScript에서 `/favorite/remove` POST 요청
   - `FavoriteService.removeFavorite()` 실행
   - `favorite` 테이블에서 해당 레코드 삭제

---

### 7. 가격 알림 기능 (`/price-alerts`)

#### 작동 흐름
1. **알림 설정**: 관심목록 페이지에서 가격 알림 설정
   - JavaScript에서 `/price-alert/set` POST 요청
   - `PriceAlertService.setPriceAlert()` 실행
   - `price_alert` 테이블에 `member_id`, `plnm_no`, `alert_price` 저장
2. **알림 체크**: 주기적으로 (또는 이벤트 발생 시) 알림 체크
   - `PriceAlertService.checkPriceAlerts()` 실행
   - 각 물건의 현재 최저매각가격과 알림 설정 가격 비교
   - 알림 조건 충족 시 이메일 발송 (Spring Mail 사용)
3. **알림 삭제**: 알림 설정 해제
   - JavaScript에서 `/price-alert/remove` POST 요청
   - `price_alert` 테이블에서 해당 레코드 삭제

---

### 8. 외부 API 연동 (온비드 API)

#### 작동 흐름
1. **API 호출**: `ApiService` 클래스에서 온비드 API 호출
   - `getUnifyUsageCltrList()`: 전체 경매물건 조회
   - `getUnifyNewCltrList()`: 신규물건 조회
   - `getUnifyDegression50PerCltrList()`: 50% 체감 물건 조회
2. **XML 파싱**: `ApiXmlParser` 유틸리티로 XML 응답 파싱
   - XML → Java 객체 변환
   - `ItemDetail` DTO로 매핑
3. **데이터 저장** (선택적): 관리자가 일괄 저장 요청 시
   - `AdminController.batchSave()` 실행
   - `item_basic`, `item_detail` 테이블에 저장

#### API 호출 예시
```java
@Service
public class ApiService {
    @Value("${onbid.serviceKey}")
    private String serviceKey;
    
    public String getUnifyUsageCltrList(int pageNo, int numOfRows, String sido) {
        String url = "http://openapi.onbid.co.kr/openapi/services/ThingInfoInquireSvc/getUnifyUsageCltr"
                + "?serviceKey=" + serviceKey
                + "&DPSL_MTD_CD=0001"
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&SIDO=" + sido;
        
        return restTemplate.getForObject(url, String.class);
    }
}
```

---

## 화면과 백엔드 연결 구조

### 1. 서버 사이드 렌더링 (SSR)

**Thymeleaf 템플릿 엔진 사용**

#### 작동 방식:
1. **컨트롤러에서 Model에 데이터 추가**
   ```java
   @GetMapping("/main")
   public String mainPage(Model model) {
       List<NoticeItem> notices = itemService.getMainPageNotices(5);
       model.addAttribute("notices", notices);
       return "main";  // templates/main.html
   }
   ```

2. **템플릿에서 데이터 사용**
   ```html
   <!-- templates/main.html -->
   <div th:each="notice : ${notices}">
       <h3 th:text="${notice.title}"></h3>
       <p th:text="${notice.date}"></p>
   </div>
   ```

3. **서버에서 HTML 생성 후 클라이언트에 전송**
   - Thymeleaf가 템플릿을 파싱하여 완성된 HTML 생성
   - 브라우저는 완성된 HTML을 받아서 바로 표시

### 2. 클라이언트 사이드 비동기 로딩 (AJAX)

**jQuery를 사용한 AJAX 요청**

#### 작동 방식:
1. **JavaScript에서 AJAX 요청**
   ```javascript
   // static/js/main.js 또는 템플릿 내부 <script>
   $.ajax({
       url: '/items/api/all-items',
       method: 'GET',
       data: { page: 1, sido: '서울특별시' },
       success: function(response) {
           // 응답 데이터를 동적으로 DOM에 추가
           renderItems(response.atList);
       }
   });
   ```

2. **REST 컨트롤러에서 JSON 응답**
   ```java
   @RestController
   @RequestMapping("/items/api")
   public class ItemRestController {
       @GetMapping("/all-items")
       public ResponseEntity<Map<String, Object>> getAllItems(
               @RequestParam int page,
               @RequestParam String sido) {
           // 데이터 조회
           List<ItemDetail> items = service.fetchAllItemsFromApi(page, sido);
           
           // JSON 응답
           Map<String, Object> response = new HashMap<>();
           response.put("success", true);
           response.put("atList", convertToAtList(items));
           return ResponseEntity.ok(response);
       }
   }
   ```

3. **JavaScript에서 동적 DOM 조작**
   ```javascript
   function renderItems(items) {
       const tbody = document.querySelector('.auction-table tbody');
       items.forEach(item => {
           const row = document.createElement('tr');
           row.innerHTML = `
               <td>${item.address}</td>
               <td>${item.minBidPrice}</td>
           `;
           tbody.appendChild(row);
       });
   }
   ```

### 3. 하이브리드 방식 (서버 사이드 + 클라이언트 사이드)

**대부분의 페이지에서 사용하는 방식**

#### 예시: 경매 물건 목록 페이지 (`/auctionList`)

1. **초기 로드**: 서버 사이드 렌더링
   - 컨트롤러에서 첫 페이지 데이터를 Model에 추가
   - Thymeleaf가 템플릿을 렌더링하여 HTML 생성
   - 브라우저는 완성된 HTML을 받아서 표시

2. **추가 로드**: 클라이언트 사이드 AJAX
   - 사용자가 페이지네이션 버튼 클릭
   - JavaScript에서 AJAX로 다음 페이지 데이터 요청
   - 응답 받은 데이터를 동적으로 테이블에 추가

#### 코드 예시:
```java
// ItemController.java
@GetMapping("/auctionList")
public String auctionListPage(Model model) {
    // 서버 사이드: 초기 데이터 로드
    List<ItemDetail> items = service.fetchAllItemsFromApi(1, "서울특별시");
    model.addAttribute("atList", convertToAtList(items));
    model.addAttribute("asyncLoad", true);  // 비동기 로딩 플래그
    
    return "item/list";
}
```

```html
<!-- templates/item/list.html -->
<table class="auction-table">
    <tbody>
        <!-- 서버 사이드 렌더링된 초기 데이터 -->
        <tr th:each="item : ${atList}">
            <td th:text="${item.address}"></td>
            <td th:text="${item.minBidPrice}"></td>
        </tr>
    </tbody>
</table>

<script>
    // 클라이언트 사이드: 추가 데이터 로드
    function loadNextPage() {
        $.ajax({
            url: '/items/api/all-items',
            data: { page: nextPage },
            success: function(response) {
                // 동적으로 테이블에 행 추가
                response.atList.forEach(item => {
                    $('tbody').append(`<tr><td>${item.address}</td></tr>`);
                });
            }
        });
    }
</script>
```

---

## 데이터 흐름

### 1. 경매 물건 조회 흐름

```
[사용자] 
    ↓ HTTP GET /auctionList
[ItemController.auctionListPage()]
    ↓
[ItemRestService.fetchAllItemsFromApi()]
    ↓
[ApiService.getUnifyUsageCltrList()]
    ↓ HTTP GET (RestTemplate)
[온비드 API]
    ↓ XML 응답
[ApiXmlParser.parse()]
    ↓
[ItemDetail DTO 객체]
    ↓
[ItemService.convertToAtList()]
    ↓
[Map<String, Object> 리스트]
    ↓ Model에 추가
[Thymeleaf 템플릿 렌더링]
    ↓ HTML 생성
[브라우저에 전송]
```

### 2. 입찰 및 결제 흐름

```
[사용자: 입찰서 작성]
    ↓ HTTP POST /payment/submit-bid (JSON)
[PaymentController.submitBid()]
    ↓
[PaymentDetailService.handleSubmitBidRequest()]
    ↓
[PaymentService.insertPaymentBase()]
    ↓ SQL INSERT
[payment_base 테이블] (status: "ready")
    ↓
[PaymentDetailService.insertPaymentDetail()]
    ↓ SQL INSERT
[payment_detail 테이블]
    ↓ JSON 응답 { success: true, paymentId: 123 }
[브라우저: 결제 페이지로 이동]
    ↓ HTTP GET /payment/checkout?paymentId=123
[PaymentController.checkoutPage()]
    ↓
[PaymentDetailService.getPaymentInfo()]
    ↓ SQL SELECT
[payment_base, payment_detail 테이블 조회]
    ↓ Model에 추가
[Thymeleaf: payment.html 렌더링]
    ↓ HTML + 아임포트 JavaScript SDK
[브라우저: 결제창 표시]
    ↓ 사용자: 결제 정보 입력
[아임포트 결제 요청]
    ↓
[아임포트 서버]
    ↓ 결제 완료 후 콜백
[HTTP POST /payment/complete]
[PaymentController.completePayment()]
    ↓
[아임포트 API: 결제 검증]
    ↓
[PaymentService.updatePaymentStatus()]
    ↓ SQL UPDATE
[payment_base 테이블] (status: "paid")
    ↓ SQL INSERT
[payment_history 테이블]
    ↓ 리다이렉트
[브라우저: /payment/success 페이지]
```

### 3. 회원 로그인 흐름

```
[사용자: 로그인 폼 제출]
    ↓ HTTP POST /login (Form Data)
[MemberController.login()]
    ↓
[MemberService.login()]
    ↓ SQL SELECT
[member 테이블 조회]
    ↓ 비밀번호 검증 (BCrypt)
[세션에 정보 저장]
    session.setAttribute("isLogin", true)
    session.setAttribute("loginId", memberId)
    ↓
[SessionAuthenticationFilter]
    ↓ 세션 정보 확인
[Spring Security 인증 객체 생성]
    ↓
[SecurityContext에 저장]
    ↓ 리다이렉트
[브라우저: /main 페이지]
```

---

## 설치 및 실행

### 필수 요구사항
- Java 21 이상
- MariaDB 10.x 이상
- Gradle 7.x 이상 (또는 Gradle Wrapper 사용)

### 환경 변수 설정

`application.properties` 파일 또는 환경 변수로 설정:

```properties
# 데이터베이스
spring.datasource.url=jdbc:mariadb://localhost:3306/allDB
spring.datasource.username=root
spring.datasource.password=your_password

# 온비드 API
onbid.serviceKey=your_onbid_service_key

# 아임포트 결제 (선택)
iamport.imp.code=your_imp_code
iamport.api.key=your_api_key
iamport.api.secret=your_api_secret
```

### 실행 방법

1. **데이터베이스 설정**
   ```bash
   # MariaDB 실행 및 데이터베이스 생성
   mysql -u root -p
   CREATE DATABASE allDB;
   ```

2. **스키마 실행**
   ```bash
   # src/main/resources/db/allDB.sql 실행
   mysql -u root -p allDB < src/main/resources/db/allDB.sql
   ```

3. **애플리케이션 실행**
   ```bash
   # Gradle Wrapper 사용
   ./gradlew bootRun
   
   # 또는 IDE에서 ApiProjApplication.main() 실행
   ```

4. **브라우저 접속**
   ```
   http://localhost:8080
   ```

### Docker를 사용한 실행

```bash
# Docker Compose로 실행 (MariaDB 포함)
docker-compose up -d

# 애플리케이션만 실행
docker build -t api-proj .
docker run -p 8080:8080 api-proj
```

---

## 주요 엔드포인트

### 공개 엔드포인트 (인증 불필요)
- `GET /` - 메인 페이지
- `GET /main` - 메인 페이지
- `GET /auctionList` - 경매 물건 목록
- `GET /api-item-detail` - 물건 상세 정보
- `GET /memberLogin` - 로그인 페이지
- `POST /login` - 로그인 처리
- `GET /memberJoin` - 회원가입 페이지
- `POST /memberJoin` - 회원가입 처리

### 인증 필요 엔드포인트
- `GET /payment/bid-form` - 입찰서 작성 페이지
- `POST /payment/submit-bid` - 입찰서 제출
- `GET /payment/checkout` - 결제 페이지
- `POST /payment/prepare` - 결제 준비
- `POST /payment/complete` - 결제 완료 콜백
- `GET /myFavorites` - 관심목록 페이지
- `POST /favorite/add` - 관심목록 추가
- `POST /favorite/remove` - 관심목록 삭제

### REST API 엔드포인트 (AJAX용)
- `GET /items/api/all-items` - 전체 경매물건 조회 (JSON)
- `GET /items/api/new-items` - 신규물건 조회 (JSON)
- `GET /items/api/item-detail` - 물건 상세 정보 조회 (JSON)

---

## 프로젝트 구조

```
src/main/
├── java/com/api/
│   ├── admin/          # 관리자 기능
│   ├── board/          # 게시판 기능
│   ├── config/         # 설정 클래스 (Security, Swagger 등)
│   ├── favorite/       # 관심목록 및 가격 알림
│   ├── info/           # 정보 페이지
│   ├── item/           # 경매 물건 관리
│   ├── member/         # 회원 관리
│   ├── payment/        # 입찰 및 결제
│   ├── union/          # 외부 API 연동
│   └── util/           # 유틸리티
└── resources/
    ├── application.properties  # 애플리케이션 설정
    ├── db/             # 데이터베이스 스키마
    ├── mapper/          # MyBatis XML 매퍼
    ├── static/          # 정적 리소스 (CSS, JS, 이미지)
    └── templates/       # Thymeleaf 템플릿 (HTML)
```

---

## 보안 설정

### Spring Security
- **세션 기반 인증**: HttpSession의 `isLogin`, `loginId` 확인
- **경로별 접근 제어**: 
  - 공개 경로: `/`, `/main`, `/auctionList` 등
  - 인증 필요: `/payment/**`, `/myFavorites` 등
- **CSRF 보호**: 프로덕션 환경에서 활성화
- **비밀번호 암호화**: BCrypt 사용

### 프로필별 설정
- **개발 환경**: 모든 경로 허용 (테스트용)
- **프로덕션 환경**: 인증 필요 경로 보호, CSRF 활성화

---

