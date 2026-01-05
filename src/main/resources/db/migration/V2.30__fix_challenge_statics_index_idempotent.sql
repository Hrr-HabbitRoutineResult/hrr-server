-- Ensure UNIQUE constraint exists
SET @exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'challenge_statics'
    AND INDEX_NAME = 'uk_challenge_favor'
);

SET @sql := IF(@exists = 0,
  'ALTER TABLE challenge_statics ADD CONSTRAINT uk_challenge_favor UNIQUE (challenge_id, favor_type, favor_value)',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure index exists
SET @idx_exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'challenge_statics'
    AND INDEX_NAME = 'idx_statics_challenge_id'
);

SET @idx_sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_statics_challenge_id ON challenge_statics (challenge_id)',
  'SELECT 1'
);

PREPARE stmt2 FROM @idx_sql;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
