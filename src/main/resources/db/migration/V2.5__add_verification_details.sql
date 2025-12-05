-- Verification 테이블에 상세 내용 컬럼 추가
ALTER TABLE verification
    ADD COLUMN type VARCHAR(20),          -- ENUM('CAMERA', 'TEXT')
    ADD COLUMN title VARCHAR(255),
    ADD COLUMN content LONGTEXT,       -- @Lob 매핑
    ADD COLUMN photo_url VARCHAR(512),
    ADD COLUMN text_url VARCHAR(512),
    ADD COLUMN is_question BIT(1),        -- Boolean 매핑
    ADD COLUMN status VARCHAR(20);        -- ENUM('TEMPORARY', 'COMPLETED')