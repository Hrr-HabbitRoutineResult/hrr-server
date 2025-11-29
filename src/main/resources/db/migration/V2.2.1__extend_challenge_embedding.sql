-- challenge_embedding 컬럼의 데이터 타입을 LONGBLOB으로 변경
ALTER TABLE challenge_embedding
    MODIFY COLUMN challenge_embedding LONGBLOB NOT NULL;
