DROP PROCEDURE IF EXISTS migrate_verification_images;

DELIMITER $$

CREATE PROCEDURE migrate_verification_images()
BEGIN
    -- 1. 새로운 JSON 저장용 컬럼 추가 (text_images가 없을 때만 실행)
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_images'
    ) THEN
ALTER TABLE verification ADD COLUMN text_images TEXT;
END IF;

    -- 2. 기존 컬럼 삭제 (text_image1이 존재할 때만 실행)
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image1'
    ) THEN
ALTER TABLE verification DROP COLUMN text_image1;
END IF;

    -- 3. 기존 컬럼 삭제 (text_image2이 존재할 때만 실행)
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image2'
    ) THEN
ALTER TABLE verification DROP COLUMN text_image2;
END IF;

    -- 4. 기존 컬럼 삭제 (text_image3이 존재할 때만 실행)
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image3'
    ) THEN
ALTER TABLE verification DROP COLUMN text_image3;
END IF;

END $$

DELIMITER ;

CALL migrate_verification_images();
DROP PROCEDURE IF EXISTS migrate_verification_images;