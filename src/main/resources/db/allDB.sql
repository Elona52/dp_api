-- 데이터베이스는 docker-compose에서 자동 생성됨
USE allDB;

CREATE TABLE item_basic (
    rnum INT,                        -- 나중에 순번 채울 컬럼
    plnm_no BIGINT PRIMARY KEY,       -- 물건번호
    address VARCHAR(500),             -- 소재지 및 내역
    appraisal_amount BIGINT,          -- 감정평가액
    min_bid_price BIGINT,             -- 최저매각가격
    org_name VARCHAR(100),            -- 담당계 / 집행기관
    bid_start DATETIME,               -- 매각기일 시작
    bid_end DATETIME,                 -- 매각기일 종료
    disposal_method VARCHAR(50),      -- 처분방식
    bid_method VARCHAR(100),          -- 입찰방식
    bid_count INT DEFAULT 1           -- 입찰 횟수
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- rnum 업데이트는 나중에 데이터가 있을 때 실행
-- SET @num := 0;
-- UPDATE item_basic SET rnum = (@num := @num + 1) ORDER BY bid_start;



CREATE TABLE item_detail (
    plnm_no BIGINT PRIMARY KEY,        -- 물건번호, item_basic와 1:1 조인
    pbct_no BIGINT,                     -- 입찰번호
    org_base_no BIGINT,                 -- 집행기관번호
    cltr_mnmt_no VARCHAR(100),          -- 관리번호
    nmr_address TEXT,           -- 지번주소
    road_name TEXT,             -- 도로명
    bld_no VARCHAR(20),                 -- 건물번호
    bid_status VARCHAR(50),             -- 입찰상태 (PBCT_CLTR_STAT_NM)
    view_count INT,                     -- 조회수 (USCBD_CNT)
    goods_detail TEXT,          -- 면적/물건 상세 (GOODS_NM)
    asset_category VARCHAR(100),        -- 처분/자산구분 (CTGR_FULL_NM)
    bid_round_no VARCHAR(20),           -- 입찰회차 (BID_MNMT_NO)
    fee_rate VARCHAR(20)               -- 수수료율 (FEE_RATE)
);

ALTER TABLE item_detail DROP COLUMN joint_bid;
ALTER TABLE item_detail DROP COLUMN electronic_guarantee;
ALTER TABLE item_detail DROP COLUMN agent_bid;


SELECT 
    b.rnum,
    b.plnm_no,
    b.address,
    b.appraisal_amount,
    b.min_bid_price,
    b.org_name,
    b.bid_start,
    b.bid_end,
    b.disposal_method,
    b.bid_method,
    d.pbct_no,
    d.org_base_no,
    d.cltr_mnmt_no,
    d.nmr_address,
    d.road_name,
    d.bld_no,
    d.bid_status,
    d.view_count,
    d.goods_detail,
    d.asset_category,
    d.bid_round_no,
    d.fee_rate
FROM item_basic b
INNER JOIN item_detail d 
    ON b.plnm_no = d.plnm_no
WHERE b.plnm_no = 754512;


-- ----------------

CREATE TABLE IF NOT EXISTS member (
    id VARCHAR(50) PRIMARY KEY COMMENT '회원 ID',
    pass VARCHAR(500) COMMENT '비밀번호 (암호화)',
    name VARCHAR(50) COMMENT '회원 이름',
    phone VARCHAR(20) COMMENT '전화번호',
    mail VARCHAR(100) COMMENT '이메일',
    zipcode INT COMMENT '우편번호',
    address1 VARCHAR(200) COMMENT '주소 1',
    address2 VARCHAR(200) COMMENT '상세주소',
    marketing VARCHAR(10) COMMENT '마케팅 수신 동의',
    joindate TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
    modificationdate TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
    type VARCHAR(20) COMMENT '회원 타입 (USER, ADMIN)'
) COMMENT '회원 정보';


ALTER TABLE member CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci, ENGINE=InnoDB;


CREATE TABLE favorite (
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(50) NOT NULL,
    item_plnm_no BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_favorite (member_id, item_plnm_no),
    CONSTRAINT fk_favorite_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_item FOREIGN KEY (item_plnm_no) REFERENCES item_basic(plnm_no) ON DELETE CASCADE,
    INDEX idx_member_id (member_id),
    INDEX idx_item_plnm_no (item_plnm_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;



-- PriceAlert 테이블 생성
CREATE TABLE IF NOT EXISTS PriceAlert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    favorite_id BIGINT,
    member_id VARCHAR(50) NOT NULL,
    item_plnm_no BIGINT NOT NULL,
    previous_price BIGINT,
    new_price BIGINT,
    alert_sent TINYINT(1) DEFAULT 0,
    sent_date TIMESTAMP NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_pricealert_favorite FOREIGN KEY (favorite_id) 
        REFERENCES favorite(favorite_id) ON DELETE SET NULL,
    CONSTRAINT fk_pricealert_member FOREIGN KEY (member_id) 
        REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT fk_pricealert_item FOREIGN KEY (item_plnm_no) 
        REFERENCES item_basic(plnm_no) ON DELETE CASCADE,
    
    INDEX idx_favorite_id (favorite_id),
    INDEX idx_member_id (member_id),
    INDEX idx_item_plnm_no (item_plnm_no),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-----------------------------

CREATE TABLE IF NOT EXISTS payment_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(50) NOT NULL,
    item_id BIGINT NOT NULL,
    bid_price BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_payment_base_member FOREIGN KEY (member_id)
        REFERENCES member(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_payment_base_item FOREIGN KEY (item_id)
        REFERENCES item_basic(plnm_no) ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_member_id (member_id),
    INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_detail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,       -- payment_base 참조
    cltr_no VARCHAR(100),
    amount BIGINT NOT NULL,
    paid_amount BIGINT,
    imp_uid VARCHAR(100) UNIQUE,
    merchant_uid VARCHAR(100) UNIQUE NOT NULL,
    item_name VARCHAR(500),
    payment_method VARCHAR(50),
    pg_provider VARCHAR(50),
    pg_tid VARCHAR(100),
    card_name VARCHAR(50),
    card_number VARCHAR(50),
    buyer_name VARCHAR(50),
    buyer_email VARCHAR(100),
    buyer_tel VARCHAR(20),
    buyer_addr VARCHAR(200),
    buyer_postcode VARCHAR(10),
    status VARCHAR(20) DEFAULT 'ready',
    paid_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    fail_reason TEXT,
    cancel_reason TEXT,
    receipt_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_payment_detail_base FOREIGN KEY (payment_id)
        REFERENCES payment_base(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_status (status),
    INDEX idx_payment_id (payment_id),
    INDEX idx_cltr_no (cltr_no),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS payment;

-- =====================================================
-- 결제 히스토리 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS payment_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '히스토리 ID',
    payment_id BIGINT NOT NULL COMMENT '결제 상세 ID (payment_detail.id 참조)',
    status VARCHAR(20) COMMENT '결제 상태',
    action VARCHAR(50) COMMENT '액션 (결제, 취소, 환불 등)',
    description TEXT COMMENT '상세 설명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    
    CONSTRAINT fk_paymenthistory_paymentdetail FOREIGN KEY (payment_id)
        REFERENCES payment_detail(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_payment_id (payment_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '결제 히스토리';

select * from payment_base;
select * from payment_detail;

select * from payment_history;

-- =====================================================
-- 게시판 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS find_board (
    no INT AUTO_INCREMENT PRIMARY KEY COMMENT '게시글 번호',
    id VARCHAR(50) NOT NULL COMMENT '작성자 ID',
    title VARCHAR(500) NOT NULL COMMENT '제목',
    content TEXT COMMENT '내용',
    category VARCHAR(50) COMMENT '구분 (real-estate, movable, site, other)',
    views INT DEFAULT 0 COMMENT '조회수',
    related_link VARCHAR(500) COMMENT '관련 링크',
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    
    CONSTRAINT fk_board_member FOREIGN KEY (id)
        REFERENCES member(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_id (id),
    INDEX idx_category (category),
    INDEX idx_reg_date (reg_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '게시판';

-- =====================================================
-- 댓글 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS reply (
    no INT AUTO_INCREMENT PRIMARY KEY COMMENT '댓글 번호',
    id VARCHAR(50) NOT NULL COMMENT '작성자 ID',
    content TEXT NOT NULL COMMENT '댓글 내용',
    reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '등록일',
    board_no INT NOT NULL COMMENT '게시글 번호',
    
    CONSTRAINT fk_reply_board FOREIGN KEY (board_no)
        REFERENCES find_board(no) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_reply_member FOREIGN KEY (id)
        REFERENCES member(id) ON DELETE CASCADE ON UPDATE CASCADE,
    
    INDEX idx_board_no (board_no),
    INDEX idx_id (id),
    INDEX idx_reg_date (reg_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '댓글';
