DROP PROCEDURE IF EXISTS RenameColumnIfExists;

DELIMITER $$
CREATE PROCEDURE RenameColumnIfExists()
BEGIN
    -- image_url 컬럼이 존재하는지 확인
    IF EXISTS (
        SELECT * FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'challenge'
        AND COLUMN_NAME = 'image_url'
    ) THEN
        -- 존재하면 변경 실행
ALTER TABLE challenge CHANGE image_url image_key VARCHAR(255);
END IF;
END $$
DELIMITER ;

CALL RenameColumnIfExists();
DROP PROCEDURE RenameColumnIfExists;