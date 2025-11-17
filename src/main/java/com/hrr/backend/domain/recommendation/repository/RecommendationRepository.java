package com.hrr.backend.domain.recommendation.repository;

import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

@Repository
@RequiredArgsConstructor
public class RecommendationRepository {

    private final JdbcTemplate jdbc;

    private static final int EMBED_DIM = 768;

    public List<ChallengeItemDto> findAllChallengeMeta() {
        String sql = """
                SELECT
                    id AS challenge_id,
                    title,
                    description,
                    category,
                    rule AS goal_text,
                    verify_start_time,
                    verify_end_time
                FROM challenge
                """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Time startSqlTime = rs.getTime("verify_start_time");
            Time endSqlTime = rs.getTime("verify_end_time");

            LocalTime start = startSqlTime != null ? startSqlTime.toLocalTime() : null;
            LocalTime end = endSqlTime != null ? endSqlTime.toLocalTime() : null;

            return ChallengeItemDto.builder()
                    .challengeId(rs.getLong("challenge_id"))
                    .title(rs.getString("title"))
                    .description(rs.getString("description"))
                    .category(rs.getString("category"))
                    .goal_text(rs.getString("goal_text"))
                    .verifyStartTime(start)
                    .verifyEndTime(end)
                    .embedding(dummyEmbedding())
                    .build();
        });
    }
    // ✅ 임시 더미 임베딩 (모든 값 0.0, 길이 768)
    private List<Float> dummyEmbedding() {
        List<Float> v = new ArrayList<>(EMBED_DIM);
        for (int i = 0; i < EMBED_DIM; i++) {
            v.add(0.0f);
        }
        return v;
    }
}
