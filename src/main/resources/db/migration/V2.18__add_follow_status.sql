-- Follow 테이블에 status 컬럼 추가 (없을 경우에만)
ALTER TABLE follow
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';