DROP PROCEDURE IF EXISTS migrate_verification_images;

DELIMITER $$

CREATE PROCEDURE migrate_verification_images()
BEGIN
    DECLARE has1 INT DEFAULT 0;
    DECLARE has2 INT DEFAULT 0;
    DECLARE has3 INT DEFAULT 0;

    -- 1) 새 컬럼 추가 (없을 때만)
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_images'
    ) THEN
ALTER TABLE verification ADD COLUMN text_images TEXT NULL;
END IF;

    -- 2) 기존 컬럼 존재 여부 체크
SELECT COUNT(*) INTO has1
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'verification'
  AND COLUMN_NAME = 'text_image1';

SELECT COUNT(*) INTO has2
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'verification'
  AND COLUMN_NAME = 'text_image2';

SELECT COUNT(*) INTO has3
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'verification'
  AND COLUMN_NAME = 'text_image3';

-- 3) 데이터 마이그레이션 (DROP 전에 반드시!)
--    text_images가 비어있는 경우에만 채움(멱등성)
IF (has1 + has2 + has3) > 0 THEN
        SET @e1 = IF(has1 = 1,
            "IF(text_image1 IS NULL OR text_image1 = '', NULL, JSON_QUOTE(text_image1))",
            "NULL"
        );
        SET @e2 = IF(has2 = 1,
            "IF(text_image2 IS NULL OR text_image2 = '', NULL, JSON_QUOTE(text_image2))",
            "NULL"
        );
        SET @e3 = IF(has3 = 1,
            "IF(text_image3 IS NULL OR text_image3 = '', NULL, JSON_QUOTE(text_image3))",
            "NULL"
        );

        SET @cond = CONCAT_WS(' OR ',
            IF(has1 = 1, "(text_image1 IS NOT NULL AND text_image1 <> '')", NULL),
            IF(has2 = 1, "(text_image2 IS NOT NULL AND text_image2 <> '')", NULL),
            IF(has3 = 1, "(text_image3 IS NOT NULL AND text_image3 <> '')", NULL)
        );

        SET @cond = IF(@cond IS NULL OR @cond = '', '0', @cond);

        SET @sql = CONCAT(
            "UPDATE verification ",
            "SET text_images = CONCAT('[', CONCAT_WS(',', ", @e1, ", ", @e2, ", ", @e3, "), ']') ",
            "WHERE (text_images IS NULL OR text_images = '' OR text_images = '[]') ",
            "AND (", @cond, ")"
        );

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END IF;

    -- 4) 기존 컬럼 삭제 (마이그레이션 완료 후)
    IF has1 = 1 THEN
ALTER TABLE verification DROP COLUMN text_image1;
END IF;

    IF has2 = 1 THEN
ALTER TABLE verification DROP COLUMN text_image2;
END IF;

    IF has3 = 1 THEN
ALTER TABLE verification DROP COLUMN text_image3;
END IF;
END $$

DELIMITER ;

CALL migrate_verification_images();
DROP PROCEDURE IF EXISTS migrate_verification_images;