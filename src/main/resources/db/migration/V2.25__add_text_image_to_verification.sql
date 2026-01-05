DROP PROCEDURE IF EXISTS add_verification_text_images;

DELIMITER $$

CREATE PROCEDURE add_verification_text_images()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image1'
    ) THEN
ALTER TABLE verification
    ADD COLUMN text_image1 VARCHAR(255) NULL;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image2'
    ) THEN
ALTER TABLE verification
    ADD COLUMN text_image2 VARCHAR(255) NULL;
END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'verification'
          AND COLUMN_NAME = 'text_image3'
    ) THEN
ALTER TABLE verification
    ADD COLUMN text_image3 VARCHAR(255) NULL;
END IF;
END $$

DELIMITER ;

CALL add_verification_text_images();
DROP PROCEDURE IF EXISTS add_verification_text_images;
