SET @exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_favor'
    AND INDEX_NAME = 'uk_user_favor_user_id'
);

SET @sql := IF(@exists = 0,
  'ALTER TABLE user_favor ADD CONSTRAINT uk_user_favor_user_id UNIQUE (user_id)',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
