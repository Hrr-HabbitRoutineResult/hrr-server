-- 인기 검색어 최종 집계 테이블 생성 (IF NOT EXISTS 추가)
CREATE TABLE IF NOT EXISTS popular_keyword (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_popular_keyword_keyword UNIQUE (keyword)
);

-- 시간별 검색어 로그 테이블 생성 (IF NOT EXISTS 추가)
CREATE TABLE IF NOT EXISTS keyword_hourly_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    count BIGINT NOT NULL,
    hour TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- 성능 최적화를 위한 인덱스 생성
-- 구분자 변경
DELIMITER $$

-- 인덱스 추가/삭제 로직을 담을 프로시저 생성
CREATE PROCEDURE EnsureKeywordIndexExists()
BEGIN
    -- 인덱스가 존재하는지 확인
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'keyword_hourly_log'
          AND INDEX_NAME = 'idx_khr_log_keyword'
    ) THEN
        -- 인덱스가 존재하지 않으면 추가
        CREATE INDEX idx_khr_log_keyword ON keyword_hourly_log (keyword);
    END IF;

    -- 다른 인덱스들도 동일하게 처리...
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'keyword_hourly_log'
          AND INDEX_NAME = 'idx_khr_log_hour'
    ) THEN
        CREATE INDEX idx_khr_log_hour ON keyword_hourly_log (hour);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'popular_keyword'
          AND INDEX_NAME = 'idx_pk_total_count'
    ) THEN
        CREATE INDEX idx_pk_total_count ON popular_keyword (total_count DESC);
    END IF;

END$$

-- 프로시저 실행
CALL EnsureKeywordIndexExists()$$

-- 프로시저 정리
DROP PROCEDURE EnsureKeywordIndexExists$$

-- 구분자 복원
DELIMITER ;
