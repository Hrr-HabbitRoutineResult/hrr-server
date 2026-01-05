SET @exists := (
  SELECT COUNT(1)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user_favor'
    AND INDEX_NAME = 'uk_user_favor_user_id'
);

-- 1) 중복 정리 (제약 없을 때만)
SET @sql1 := IF(@exists = 0,
'DELETE uf
 FROM user_favor uf
 JOIN (
   SELECT user_id, MAX(id) AS keep_id
   FROM user_favor
   GROUP BY user_id
 ) k ON k.user_id = uf.user_id
 WHERE uf.id <> k.keep_id;',
'SELECT 1');

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- 2) 제약 추가 (제약 없을 때만)
SET @sql2 := IF(@exists = 0,
'ALTER TABLE user_favor
   ADD CONSTRAINT uk_user_favor_user_id UNIQUE (user_id);',
'SELECT 1');

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
