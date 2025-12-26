# 데이터베이스 ERD (Entity Relationship Diagram)

이 문서는 공공 부동산 경매 플랫폼의 데이터베이스 구조를 Mermaid 다이어그램으로 표현합니다.

## ERD 다이어그램

```mermaid
erDiagram
    member ||--o{ favorite : "has"
    member ||--o{ payment_base : "makes"
    member ||--o{ find_board : "writes"
    member ||--o{ reply : "writes"
    member ||--o{ PriceAlert : "sets"
    
    item_basic ||--|| item_detail : "has"
    item_basic ||--o{ favorite : "included_in"
    item_basic ||--o{ payment_base : "bid_for"
    item_basic ||--o{ PriceAlert : "monitored_by"
    
    payment_base ||--|| payment_detail : "has"
    payment_detail ||--o{ payment_history : "has"
    
    find_board ||--o{ reply : "has"
    
    favorite ||--o{ PriceAlert : "may_have"

    member {
        VARCHAR id PK "회원 ID"
        VARCHAR pass "비밀번호 (암호화)"
        VARCHAR name "회원 이름"
        VARCHAR phone "전화번호"
        VARCHAR mail "이메일"
        INT zipcode "우편번호"
        VARCHAR address1 "주소 1"
        VARCHAR address2 "상세주소"
        VARCHAR marketing "마케팅 수신 동의"
        TIMESTAMP joindate "가입일"
        TIMESTAMP modificationdate "수정일"
        VARCHAR type "회원 타입 (USER, ADMIN)"
    }
    
    item_basic {
        INT rnum "순번"
        BIGINT plnm_no PK "물건번호"
        VARCHAR address "소재지 및 내역"
        BIGINT appraisal_amount "감정평가액"
        BIGINT min_bid_price "최저매각가격"
        VARCHAR org_name "담당계 / 집행기관"
        DATETIME bid_start "매각기일 시작"
        DATETIME bid_end "매각기일 종료"
        VARCHAR disposal_method "처분방식"
        VARCHAR bid_method "입찰방식"
        INT bid_count "입찰 횟수"
    }
    
    item_detail {
        BIGINT plnm_no PK "물건번호 (FK)"
        BIGINT pbct_no "입찰번호"
        BIGINT org_base_no "집행기관번호"
        VARCHAR cltr_mnmt_no "관리번호"
        TEXT nmr_address "지번주소"
        TEXT road_name "도로명"
        VARCHAR bld_no "건물번호"
        VARCHAR bid_status "입찰상태"
        INT view_count "조회수"
        TEXT goods_detail "면적/물건 상세"
        VARCHAR asset_category "처분/자산구분"
        VARCHAR bid_round_no "입찰회차"
        VARCHAR fee_rate "수수료율"
    }
    
    favorite {
        BIGINT favorite_id PK "관심목록 ID"
        VARCHAR member_id FK "회원 ID"
        BIGINT item_plnm_no FK "물건번호"
        TIMESTAMP created_at "생성일"
    }
    
    PriceAlert {
        BIGINT id PK "알림 ID"
        BIGINT favorite_id FK "관심목록 ID (nullable)"
        VARCHAR member_id FK "회원 ID"
        BIGINT item_plnm_no FK "물건번호"
        BIGINT previous_price "이전 가격"
        BIGINT new_price "새로운 가격"
        TINYINT alert_sent "알림 전송 여부"
        TIMESTAMP sent_date "전송 날짜"
        TIMESTAMP created_date "생성일"
    }
    
    payment_base {
        BIGINT id PK "입찰 ID"
        VARCHAR member_id FK "회원 ID"
        BIGINT item_id FK "물건번호"
        BIGINT bid_price "입찰 금액"
        DATETIME created_at "생성일"
    }
    
    payment_detail {
        BIGINT id PK "결제 상세 ID"
        BIGINT payment_id FK "입찰 ID"
        VARCHAR cltr_no "관리번호"
        BIGINT amount "결제 예정 금액"
        BIGINT paid_amount "실제 결제 금액"
        VARCHAR imp_uid "아임포트 고유 결제번호"
        VARCHAR merchant_uid "가맹점 주문번호"
        VARCHAR item_name "상품명"
        VARCHAR payment_method "결제 수단"
        VARCHAR pg_provider "PG사"
        VARCHAR pg_tid "PG사 거래번호"
        VARCHAR card_name "카드사 명"
        VARCHAR card_number "카드 번호"
        VARCHAR buyer_name "구매자 이름"
        VARCHAR buyer_email "구매자 이메일"
        VARCHAR buyer_tel "구매자 전화번호"
        VARCHAR buyer_addr "구매자 주소"
        VARCHAR buyer_postcode "구매자 우편번호"
        VARCHAR status "결제 상태 (ready, paid, failed, cancelled)"
        TIMESTAMP paid_at "결제 완료 시간"
        TIMESTAMP failed_at "결제 실패 시간"
        TIMESTAMP cancelled_at "결제 취소 시간"
        TEXT fail_reason "결제 실패 사유"
        TEXT cancel_reason "결제 취소 사유"
        VARCHAR receipt_url "영수증 URL"
        TIMESTAMP created_at "생성일"
        TIMESTAMP updated_at "수정일"
    }
    
    payment_history {
        BIGINT id PK "히스토리 ID"
        BIGINT payment_id FK "결제 상세 ID"
        VARCHAR status "결제 상태"
        VARCHAR action "액션 (결제, 취소, 환불 등)"
        TEXT description "상세 설명"
        TIMESTAMP created_at "생성일"
    }
    
    find_board {
        INT no PK "게시글 번호"
        VARCHAR id FK "작성자 ID"
        VARCHAR title "제목"
        TEXT content "내용"
        VARCHAR category "구분 (real-estate, movable, site, other)"
        INT views "조회수"
        VARCHAR related_link "관련 링크"
        TIMESTAMP reg_date "등록일"
    }
    
    reply {
        INT no PK "댓글 번호"
        VARCHAR id FK "작성자 ID"
        TEXT content "댓글 내용"
        TIMESTAMP reg_date "등록일"
        INT board_no FK "게시글 번호"
    }
```

## 테이블 관계 설명

### 1. 회원 관련 관계
- **member ↔ favorite**: 한 회원은 여러 관심목록을 가질 수 있음 (1:N)
- **member ↔ payment_base**: 한 회원은 여러 입찰을 할 수 있음 (1:N)
- **member ↔ find_board**: 한 회원은 여러 게시글을 작성할 수 있음 (1:N)
- **member ↔ reply**: 한 회원은 여러 댓글을 작성할 수 있음 (1:N)
- **member ↔ PriceAlert**: 한 회원은 여러 가격 알림을 설정할 수 있음 (1:N)

### 2. 물건 관련 관계
- **item_basic ↔ item_detail**: 물건 기본 정보와 상세 정보는 1:1 관계
- **item_basic ↔ favorite**: 한 물건은 여러 회원의 관심목록에 포함될 수 있음 (1:N)
- **item_basic ↔ payment_base**: 한 물건은 여러 입찰 대상이 될 수 있음 (1:N)
- **item_basic ↔ PriceAlert**: 한 물건은 여러 가격 알림 대상이 될 수 있음 (1:N)

### 3. 결제 관련 관계
- **payment_base ↔ payment_detail**: 입찰 기본 정보와 결제 상세 정보는 1:1 관계
- **payment_detail ↔ payment_history**: 결제 상세 정보는 여러 이력을 가질 수 있음 (1:N)

### 4. 게시판 관련 관계
- **find_board ↔ reply**: 한 게시글은 여러 댓글을 가질 수 있음 (1:N)

### 5. 관심목록 관련 관계
- **favorite ↔ PriceAlert**: 관심목록은 가격 알림을 가질 수 있음 (1:N, nullable)

## 주요 인덱스

### member 테이블
- PRIMARY KEY: `id`

### item_basic 테이블
- PRIMARY KEY: `plnm_no`

### item_detail 테이블
- PRIMARY KEY: `plnm_no` (item_basic.plnm_no와 동일)

### favorite 테이블
- PRIMARY KEY: `favorite_id`
- UNIQUE KEY: `(member_id, item_plnm_no)` - 중복 방지
- INDEX: `idx_member_id`, `idx_item_plnm_no`

### PriceAlert 테이블
- PRIMARY KEY: `id`
- INDEX: `idx_favorite_id`, `idx_member_id`, `idx_item_plnm_no`, `idx_created_date`

### payment_base 테이블
- PRIMARY KEY: `id`
- INDEX: `idx_member_id`, `idx_item_id`

### payment_detail 테이블
- PRIMARY KEY: `id`
- UNIQUE KEY: `imp_uid`, `merchant_uid`
- INDEX: `idx_status`, `idx_payment_id`, `idx_cltr_no`, `idx_created_at`

### payment_history 테이블
- PRIMARY KEY: `id`
- INDEX: `idx_payment_id`, `idx_created_at`

### find_board 테이블
- PRIMARY KEY: `no`
- INDEX: `idx_id`, `idx_category`, `idx_reg_date`

### reply 테이블
- PRIMARY KEY: `no`
- INDEX: `idx_board_no`, `idx_id`, `idx_reg_date`

## 외래 키 제약조건

### CASCADE 정책
- **ON DELETE CASCADE**: 부모 레코드 삭제 시 자식 레코드도 함께 삭제
  - member 삭제 → favorite, payment_base, find_board, reply, PriceAlert 자동 삭제
  - item_basic 삭제 → favorite, payment_base, PriceAlert 자동 삭제
  - payment_base 삭제 → payment_detail 자동 삭제
  - payment_detail 삭제 → payment_history 자동 삭제
  - find_board 삭제 → reply 자동 삭제

- **ON DELETE SET NULL**: 부모 레코드 삭제 시 자식 레코드의 외래 키를 NULL로 설정
  - favorite 삭제 → PriceAlert.favorite_id가 NULL로 설정 (히스토리 보존)

## 데이터 흐름 예시

### 입찰 및 결제 흐름
```
1. member (회원) 
   ↓
2. payment_base (입찰 기본 정보 생성)
   ↓
3. payment_detail (결제 상세 정보 생성, status: "ready")
   ↓
4. payment_history (결제 이력 기록)
   ↓
5. payment_detail (status: "paid"로 업데이트)
```

### 관심목록 및 알림 흐름
```
1. member (회원)
   ↓
2. favorite (관심목록 추가)
   ↓
3. PriceAlert (가격 알림 설정, optional)
   ↓
4. 가격 변동 감지 시 알림 발송
```

## 참고사항

- 모든 테이블은 `utf8mb4` 문자셋과 `utf8mb4_unicode_ci` 콜레이션을 사용합니다.
- 모든 테이블은 `InnoDB` 엔진을 사용합니다.
- 타임스탬프 필드는 자동으로 생성/업데이트됩니다 (`DEFAULT CURRENT_TIMESTAMP`, `ON UPDATE CURRENT_TIMESTAMP`).
- 결제 상태는 `ready`, `paid`, `failed`, `cancelled` 값을 가질 수 있습니다.

