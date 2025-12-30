-- erification 테이블에 report_count 컬럼 추가 및 기본값 설정
DROP PROCEDURE IF EXISTS AddColumnIfNotExist;
DELIMITER //
CREATE PROCEDURE AddColumnIfNotExist()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME = 'verification' AND COLUMN_NAME = 'report_count'
    ) THEN
        ALTER TABLE verification ADD COLUMN report_count INT DEFAULT 0 NOT NULL;
    END IF;
END //
DELIMITER ;
CALL AddColumnIfNotExist();
DROP PROCEDURE AddColumnIfNotExist;

-- verification_post_report 테이블 생성 (존재하지 않을 경우에만)
CREATE TABLE IF NOT EXISTS verification_post_report (
                                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                        reporter_id BIGINT NOT NULL,
                                                        verification_id BIGINT,
                                                        reason VARCHAR(50) NOT NULL,
                                                        description TEXT,
                                                        status VARCHAR(50) DEFAULT 'PENDING',
                                                        created_at DATETIME(6) NOT NULL,
                                                        updated_at DATETIME(6) NOT NULL,
                                                        CONSTRAINT fk_report_verification_id FOREIGN KEY (verification_id) REFERENCES verification (id)
) ENGINE=InnoDB;
