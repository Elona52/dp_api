-- =====================================================
-- 게시판 테이블 생성
-- =====================================================
USE allDB;

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
-- 댓글 테이블 생성
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

