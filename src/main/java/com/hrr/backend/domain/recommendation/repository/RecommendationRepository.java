package com.hrr.backend.domain.recommendation.repository;

import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

@Repository
@RequiredArgsConstructor
public class RecommendationRepository {

    private final JdbcTemplate jdbc;

    private static final int EMBED_DIM = 768;

    public List<ChallengeItemDto> findAllChallengeMeta() {
        String sql = """
                SELECT
                    c.id AS challenge_id,
                    c.title,
                    c.description,
                    c.category,
                    c.rule AS goal_text,
                    c.verify_start_time,
                    c.verify_end_time,
                    ce.challenge_embedding
                FROM challenge c
                LEFT JOIN challenge_embedding ce
                    ON ce.challenge_id = c.id
                """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Time startSqlTime = rs.getTime("verify_start_time");
            Time endSqlTime = rs.getTime("verify_end_time");

            LocalTime start = startSqlTime != null ? startSqlTime.toLocalTime() : null;
            LocalTime end = endSqlTime != null ? endSqlTime.toLocalTime() : null;

            byte[] embeddingBytes = rs.getBytes("challenge_embedding");

            return ChallengeItemDto.builder()
                    .challengeId(rs.getLong("challenge_id"))
                    .title(rs.getString("title"))
                    .description(rs.getString("description"))
                    .category(rs.getString("category"))
                    .goal_text(rs.getString("goal_text"))
                    .verifyStartTime(start)
                    .verifyEndTime(end)
                    .embedding(bytesToFloatList(embeddingBytes))
                    .build();
        });
    }

    private List<Float> bytesToFloatList(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        int len = bytes.length / 4;
        List<Float> list = new ArrayList<>(len);

        for (int i = 0; i < len; i++) {
            list.add(buffer.getFloat());
        }

        if (list.size() != EMBED_DIM) {
            throw new GlobalException(ErrorCode.EMBEDDING_LENGTH_ERROR);
        }
        return list;
    }
}
