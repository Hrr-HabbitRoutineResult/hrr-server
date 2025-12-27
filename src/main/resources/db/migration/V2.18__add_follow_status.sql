-- Follow 테이블에 status 컬럼 추가
ALTER TABLE follow ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';