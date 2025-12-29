-- 1. social_auth 테이블 생성
CREATE TABLE IF NOT EXISTS social_auth (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           user_id BIGINT NOT NULL,
                                           social_type VARCHAR(20) NOT NULL,
                                           social_id VARCHAR(255) NOT NULL,
                                           social_refresh_token TEXT,
                                           created_at DATETIME(6),
                                           updated_at DATETIME(6),
                                           CONSTRAINT fk_social_auth_user FOREIGN KEY (user_id) REFERENCES user (id),
                                           UNIQUE KEY uq_social_id_type (social_id, social_type)
) ENGINE=InnoDB;

-- 2. 안전한 데이터 이전 및 컬럼 삭제를 위한 통합 프로시저
DROP PROCEDURE IF EXISTS MigrateAndDropSocialColumns;

DELIMITER //

CREATE PROCEDURE MigrateAndDropSocialColumns()
BEGIN
    -- [A] 데이터 이전 : user 테이블에 social_id 컬럼이 아직 있을 때만 수행
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'social_id') THEN

        -- social_auth가 비어있는 유저들만 이전 (중복 방지 )
        INSERT INTO social_auth (user_id, social_id, social_type, created_at, updated_at)
        SELECT id, CAST(social_id AS CHAR), social_type, NOW(), NOW()
        FROM user
        WHERE social_id IS NOT NULL
          AND id NOT IN (SELECT user_id FROM social_auth);

        -- 이전 완료 후 컬럼 삭제
        ALTER TABLE user DROP COLUMN social_id;
    END IF;

    -- [B] social_type 컬럼 삭제 (남아있을 경우)
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'social_type') THEN
        ALTER TABLE user DROP COLUMN social_type;
    END IF;

    -- [C] 만약 kakao_id 컬럼명이 남아있는 경우를 대비한 추가 처리
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'kakao_id') THEN
        ALTER TABLE user DROP COLUMN kakao_id;
    END IF;
END //

DELIMITER ;

-- 프로시저 실행
CALL MigrateAndDropSocialColumns();

-- 프로시저 삭제
DROP PROCEDURE IF EXISTS MigrateAndDropSocialColumns;
