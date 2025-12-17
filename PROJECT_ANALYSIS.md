# ApiProj 프로젝트 분석 보고서

## 📋 프로젝트 개요

**ApiProj**는 공공 부동산 경매 정보를 통합 관리하고 사용자가 입찰 및 결제를 진행할 수 있는 Spring Boot 기반의 웹 애플리케이션입니다. 온비드(Onbid) 공공 부동산 경매 API를 연동하여 실시간 경매 정보를 수집하고, 사용자에게 직관적인 인터페이스를 제공합니다.

### 프로젝트 목적
- 공공 부동산 경매 물건 정보의 통합 조회 및 관리
- 사용자 친화적인 경매 물건 검색 및 필터링
- 입찰 신청 및 결제 시스템 구축
- 관심 물건 관리 및 가격 알림 기능 제공

---

## 🏗️ 시스템 아키텍처

### 기술 스택

#### 백엔드 프레임워크
- **Spring Boot 3.5.9-SNAPSHOT** - 메인 프레임워크
- **Java 21** - 프로그래밍 언어
- **Spring MVC** - 웹 애플리케이션 계층
- **Spring Security** - 인증 및 보안
- **MyBatis 3.0.5** - ORM 프레임워크
- **Thymeleaf** - 서버 사이드 템플릿 엔진

#### 데이터베이스
- **MariaDB** - 주 데이터베이스

#### 외부 API 연동
- **온비드(Onbid) API** - 공공 부동산 경매 정보 수집
- **아임포트(Iamport)** - 결제 시스템 연동

#### 개발 도구
- **Lombok** - 보일러플레이트 코드 제거
- **Swagger/OpenAPI 2.3.0** - API 문서화
- **Spring Boot DevTools** - 개발 편의성

#### 배포 환경
- **Docker** - 컨테이너화
- **Docker Compose** - 로컬 개발 환경
- **Render** - 클라우드 배포 플랫폼

---

## 📁 프로젝트 구조

```
ApiProj/
├── src/main/java/com/api/
│   ├── admin/              # 관리자 기능
│   │   ├── controller/    # AdminRestController
│   │   ├── domain/         # 응답 DTO
│   │   └── service/        # AdminService
│   ├── board/              # 게시판 기능
│   │   ├── controller/     # BoardController
│   │   ├── domain/         # FindBoard, Reply
│   │   ├── mapper/         # BoardMapper
│   │   └── service/        # boardService
│   ├── config/             # 설정 클래스
│   │   ├── IamportConfig   # 아임포트 설정
│   │   ├── RestTemplateConfig  # HTTP 클라이언트 설정
│   │   ├── SecurityConfig  # 보안 설정
│   │   ├── SwaggerConfig   # API 문서화 설정
│   │   └── WebMvcConfig    # 웹 MVC 설정
│   ├── favorite/           # 관심목록 기능
│   │   ├── controller/    # FavoriteController
│   │   ├── domain/         # Favorite, PriceAlert
│   │   ├── mapper/         # FavoriteMapper
│   │   └── service/        # FavoriteService, PriceAlertService
│   ├── info/               # 정보 페이지
│   │   └── controller/    # InfoController
│   ├── item/               # 경매 물건 관리
│   │   ├── controller/     # ItemController, ItemRestController
│   │   ├── domain/         # Item
│   │   ├── dto/            # ItemBasic, ItemDetail
│   │   ├── mapper/         # ItemMapper
│   │   └── service/        # ItemService, ItemRestService
│   ├── member/             # 회원 관리
│   │   ├── controller/     # MemberController, MemberRestController
│   │   ├── domain/         # Member
│   │   ├── dto/            # MemberJoinRequest, MemberLoginRequest, MemberUpdateRequest
│   │   ├── mapper/         # MemberMapper
│   │   └── service/        # MemberService
│   ├── payment/            # 결제 및 입찰
│   │   ├── controller/     # PaymentController
│   │   ├── domain/         # PaymentBase, PaymentDetail, PaymentHistory
│   │   ├── dto/            # PaymentConverter, PaymentResponse
│   │   ├── mapper/         # PaymentMapper
│   │   ├── service/        # PaymentService, PaymentDetailService
│   │   └── util/           # PaymentKeyGenerator
│   ├── union/              # 외부 API 통합
│   │   ├── controller/     # ApiController
│   │   ├── dto/            # ItemBasic, ItemDetail (API 응답용)
│   │   └── service/        # ApiService, ItemFetchService
│   └── util/               # 유틸리티
│       └── ApiXmlParser    # XML 파싱 유틸리티
├── src/main/resources/
│   ├── application.properties  # 애플리케이션 설정
│   ├── db/                    # 데이터베이스 스키마
│   │   ├── allDB.sql          # 메인 데이터베이스 스키마
│   │   └── create_board_tables.sql  # 게시판 테이블
│   ├── mapper/                # MyBatis XML 매퍼
│   │   ├── apiMapper.xml
│   │   ├── boardMapper.xml
│   │   ├── favoriteMapper.xml
│   │   ├── memberMapper.xml
│   │   └── paymentMapper.xml
│   ├── static/                # 정적 리소스
│   │   ├── css/               # 스타일시트
│   │   ├── img/               # 이미지
│   │   ├── js/                # JavaScript
│   │   └── info/               # 정보 이미지
│   └── templates/             # Thymeleaf 템플릿
│       ├── admin/             # 관리자 페이지
│       ├── board/             # 게시판
│       ├── info/              # 정보 페이지
│       ├── item/              # 물건 관련 페이지
│       ├── layout/            # 레이아웃
│       ├── member/            # 회원 관련 페이지
│       └── payment/           # 결제 관련 페이지
└── Docker 관련 파일
    ├── Dockerfile             # 프로덕션 빌드용
    ├── docker-compose.yml     # 로컬 개발 환경
    └── render.yaml            # Render 배포 설정
```

---

## 🔑 주요 기능 모듈

### 1. 경매 물건 관리 (Item Module)

#### 기능
- **공공 API 연동**: 온비드 API를 통한 실시간 경매 정보 수집
- **물건 조회**: 카테고리, 지역, 키워드 기반 검색
- **상세 정보**: 물건별 상세 정보 및 입찰 이력 조회
- **필터링**: 신규물건, 50% 체감 물건, 금주 마감 물건 필터

#### 주요 엔드포인트
- `GET /` 또는 `GET /main` - 메인 페이지 (신규물건, 통계, 일정)
- `GET /auctionList` - 경매 물건 목록
- `GET /new-items` - 신규물건 목록
- `GET /discount-50` - 50% 체감 물건 목록
- `GET /api-item-detail` - 물건 상세 정보

#### 데이터베이스 테이블
- `item_basic`: 물건 기본 정보 (물건번호, 주소, 감정평가액, 최저매각가격 등)
- `item_detail`: 물건 상세 정보 (관리번호, 입찰상태, 면적, 자산구분 등)

---

### 2. 회원 관리 (Member Module)

#### 기능
- **회원가입**: 신규 회원 등록
- **로그인/로그아웃**: 세션 기반 인증
- **회원정보 수정**: 개인정보 변경
- **아이디/비밀번호 찾기**: 이메일 기반 복구

#### 주요 엔드포인트
- `GET /memberLogin` - 로그인 페이지
- `POST /login` - 로그인 처리
- `GET /logout` - 로그아웃
- `GET /memberJoin` - 회원가입 페이지
- `POST /memberJoin` - 회원가입 처리
- `GET /memberUpdate` - 회원정보 수정 페이지
- `POST /memberUpdate` - 회원정보 수정 처리
- `POST /findId` - 아이디 찾기
- `POST /findPassword` - 비밀번호 찾기
- `POST /resetPassword` - 비밀번호 재설정

#### 데이터베이스 테이블
- `member`: 회원 정보 (아이디, 비밀번호, 이름, 전화번호, 이메일 등)

---

### 3. 입찰 및 결제 (Payment Module)

#### 기능
- **입찰서 작성**: 경매 물건에 대한 입찰 신청
- **결제 처리**: 아임포트를 통한 결제 연동
- **입찰 내역 관리**: 사용자별 입찰 내역 조회 및 관리
- **결제 상태 관리**: 입찰, 결제, 취소 상태 추적

#### 주요 엔드포인트
- `GET /payment/bid-form` - 입찰서 작성 페이지
- `POST /payment/submit-bid` - 입찰서 제출
- `GET /payment/checkout` - 결제 페이지
- `POST /payment/prepare` - 결제 준비 (주문번호 생성)
- `POST /payment/complete` - 결제 완료 (아임포트 콜백)
- `GET /payment/success` - 결제 성공 페이지
- `GET /payment/fail` - 결제 실패 페이지
- `GET /payment/my-payments` - 내 결제 내역
- `GET /payment/detail/{paymentId}` - 결제 상세 정보
- `POST /payment/cancel/{paymentId}` - 결제 취소
- `DELETE /payment/delete/{paymentId}` - 입찰 내역 삭제

#### 데이터베이스 테이블
- `payment_base`: 입찰 기본 정보 (주문번호, 회원ID, 물건번호, 입찰금액 등)
- `payment_detail`: 입찰 상세 정보 (입찰방식, 결제방법, 환불계좌 등)
- `payment_history`: 결제 이력 (결제상태, 결제일시, 취소사유 등)

---

### 4. 관심목록 및 알림 (Favorite Module)

#### 기능
- **관심목록 추가/삭제**: 관심 있는 경매 물건 관리
- **가격 알림**: 설정한 가격 이하로 떨어질 때 이메일 알림
- **관심목록 조회**: 사용자별 관심 물건 목록

#### 주요 엔드포인트
- `POST /favorite/add` - 관심목록 추가
- `POST /favorite/remove` - 관심목록 삭제
- `GET /myFavorites` - 관심목록 페이지
- `POST /price-alert/set` - 가격 알림 설정
- `POST /price-alert/remove` - 가격 알림 삭제

#### 데이터베이스 테이블
- `favorite`: 관심목록 (회원ID, 물건번호)
- `price_alert`: 가격 알림 (회원ID, 물건번호, 알림가격)

---

### 5. 게시판 (Board Module)

#### 기능
- **FAQ 게시판**: 자주 묻는 질문 관리
- **게시글 작성/수정/삭제**: 관리자 게시글 관리
- **댓글 기능**: 게시글별 댓글 작성 및 관리

#### 주요 엔드포인트
- `GET /board/faq` - FAQ 목록
- `GET /board/write` - 게시글 작성 페이지
- `POST /board/write` - 게시글 작성
- `POST /board/reply` - 댓글 작성

#### 데이터베이스 테이블
- `find_board`: 게시글 (제목, 내용, 작성자, 작성일시 등)
- `reply`: 댓글 (게시글ID, 내용, 작성자, 작성일시 등)

---

### 6. 관리자 기능 (Admin Module)

#### 기능
- **물건 일괄 저장**: API에서 수집한 물건 정보 일괄 저장
- **회원 관리**: 회원 목록 조회 및 관리
- **시스템 관리**: 운영 관련 기능

#### 주요 엔드포인트
- `POST /admin/batch-save` - 물건 일괄 저장
- `GET /admin/members` - 회원 목록
- `GET /admin/items` - 물건 목록 관리

---

## 🔌 외부 API 연동

### 1. 온비드(Onbid) API

#### 용도
공공 부동산 경매 정보 수집

#### 연동 방식
- **REST API**: XML 형식 응답
- **인증**: 서비스 키 기반 인증
- **데이터 파싱**: `ApiXmlParser`를 통한 XML → 객체 변환

#### 주요 API 엔드포인트
- 물건 목록 조회
- 물건 상세 정보 조회
- 입찰 이력 조회

#### 설정
```properties
onbid.serviceKey=${ONBID_SERVICE_KEY:...}
```

---

### 2. 아임포트(Iamport) 결제 API

#### 용도
입찰 보증금 및 결제 처리

#### 연동 방식
- **REST API**: JSON 형식
- **인증**: API Key, Secret 기반
- **결제 플로우**: 
  1. 결제 준비 (주문번호 생성)
  2. 결제 요청 (프론트엔드)
  3. 결제 완료 콜백 (서버)

#### 설정
```properties
iamport.imp.code=${IAMPORT_IMP_CODE:...}
iamport.api.key=${IAMPORT_API_KEY:...}
iamport.api.secret=${IAMPORT_API_SECRET:...}
iamport.callback.url=${IAMPORT_CALLBACK_URL:...}
```

---

## 🗄️ 데이터베이스 설계

### 주요 테이블 구조

#### item_basic (물건 기본 정보)
- `plnm_no` (BIGINT, PK) - 물건번호
- `address` (VARCHAR) - 소재지 및 내역
- `appraisal_amount` (BIGINT) - 감정평가액
- `min_bid_price` (BIGINT) - 최저매각가격
- `org_name` (VARCHAR) - 담당계/집행기관
- `bid_start` (DATETIME) - 매각기일 시작
- `bid_end` (DATETIME) - 매각기일 종료
- `bid_count` (INT) - 입찰 횟수

#### item_detail (물건 상세 정보)
- `plnm_no` (BIGINT, PK) - 물건번호 (item_basic과 1:1)
- `cltr_mnmt_no` (VARCHAR) - 관리번호
- `nmr_address` (TEXT) - 지번주소
- `road_name` (TEXT) - 도로명
- `bid_status` (VARCHAR) - 입찰상태
- `goods_detail` (TEXT) - 면적/물건 상세
- `asset_category` (VARCHAR) - 처분/자산구분

#### member (회원 정보)
- `id` (VARCHAR, PK) - 회원 아이디
- `password` (VARCHAR) - 비밀번호
- `name` (VARCHAR) - 이름
- `phone` (VARCHAR) - 전화번호
- `email` (VARCHAR) - 이메일

#### payment_base (입찰 기본 정보)
- `payment_id` (BIGINT, PK) - 입찰ID
- `member_id` (VARCHAR) - 회원ID
- `plnm_no` (BIGINT) - 물건번호
- `merchant_uid` (VARCHAR) - 주문번호
- `bid_amount` (BIGINT) - 입찰금액
- `deposit_amount` (BIGINT) - 보증금
- `payment_status` (VARCHAR) - 결제상태

---

## 🔒 보안 설정

### Spring Security
- **세션 기반 인증**: 로그인 상태 관리
- **경로별 접근 제어**: 
  - 공개 경로: `/`, `/main`, `/auctionList`, `/api-item-detail`
  - 인증 필요: `/payment/**`, `/myFavorites`, `/memberUpdate`
- **CSRF 보호**: 활성화
- **세션 타임아웃**: 기본 설정

### 프로필별 설정
- **개발 환경**: 모든 경로 허용 (테스트용)
- **프로덕션 환경**: 인증 필요 경로 보호

---

## 🚀 배포 환경

### Docker 배포
- **Dockerfile**: 멀티 스테이지 빌드
- **docker-compose.yml**: 로컬 개발 환경 (MariaDB 포함)

### Render 배포
- **render.yaml**: Render Blueprint 설정
- **환경 변수**: 데이터베이스 연결 정보, API 키 등

---

## 📊 성능 최적화

### 데이터베이스
- **인덱스**: 주요 조회 컬럼에 인덱스 설정
- **페이징**: 목록 조회 시 페이징 처리
- **MyBatis 캐싱**: 쿼리 결과 캐싱

### 애플리케이션
- **로깅**: SLF4J + Logback
- **예외 처리**: 전역 예외 핸들러
- **비동기 처리**: 이메일 발송 등

---

## 🧪 테스트

### 테스트 구조
- **단위 테스트**: JUnit 5
- **통합 테스트**: Spring Boot Test
- **MyBatis 테스트**: MyBatis Spring Boot Test

---

## 📝 주요 개선 사항

### 최근 수정 내역
1. **입찰 시스템 개선**: 입찰서 작성 → 제출 → 결제 플로우 개선
2. **결제 연동**: 아임포트 결제 시스템 통합
3. **관심목록 기능**: cltrNo 기반 관심목록 추가 개선
4. **템플릿 오류 수정**: Thymeleaf 템플릿 오류 해결
5. **보안 설정**: 프로덕션 환경 보안 강화

---

## 🔮 향후 개선 방향

### 기능 개선
- [ ] 실시간 알림 시스템 (WebSocket)
- [ ] 모바일 앱 지원 (REST API 확장)
- [ ] 고급 검색 필터 (지도 기반 검색)
- [ ] 입찰 자동화 기능

### 기술 개선
- [ ] Redis 캐싱 도입
- [ ] Elasticsearch 검색 엔진 통합
- [ ] 마이크로서비스 아키텍처 전환 검토
- [ ] CI/CD 파이프라인 구축

---

## 📚 참고 자료

### 프로젝트 문서
- `README.md` - 프로젝트 개요 및 실행 방법
- `docs/` - 추가 문서 (데이터베이스 설정, 배포 가이드 등)

### 프로젝트 프레젠테이션
- [공공 API 프로젝트 프레젠테이션](https://docs.google.com/presentation/d/1iSdWrPILqVFreVf4YNEgncYHgLUfjnEtRQzJY-Avvjw/edit?usp=sharing) - 프로젝트 소개 및 기능 설명

### 외부 문서
- [온비드 API 문서](https://www.onbid.co.kr/)
- [아임포트 개발자 문서](https://developers.iamport.kr/)

---

## 📞 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.

---

**작성일**: 2025년 12월  
**프로젝트 버전**: 0.0.1-SNAPSHOT  
**Spring Boot 버전**: 3.5.9-SNAPSHOT  
**Java 버전**: 21

