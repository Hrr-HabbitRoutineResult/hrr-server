-- 인기 검색어 최종 집계 테이블 생성
CREATE TABLE popular_keyword (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_popular_keyword_keyword UNIQUE (keyword)
);

-- 시간별 검색어 로그 테이블 생성
CREATE TABLE keyword_hourly_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    keyword VARCHAR(255) NOT NULL,
    count BIGINT NOT NULL,
    hour TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- 성능 최적화를 위한 인덱스 생성

CREATE INDEX idx_khr_log_keyword ON keyword_hourly_log (keyword);
CREATE INDEX idx_khr_log_hour ON keyword_hourly_log (hour);

CREATE INDEX idx_pk_total_count ON popular_keyword (total_count DESC);
