package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeEmbedding;
import com.hrr.backend.domain.challenge.repository.ChallengeEmbeddingRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeEmbeddingAsyncService {
    private static final int EMBEDDING_DIM = 768;

    private final ChallengeRepository challengeRepository;
    private final ChallengeEmbeddingRepository challengeEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateAndSaveEmbedding(Long challengeId, String challengeText) {
        try {
            float[] embedding = embeddingClient.getEmbedding(challengeText);

            if (embedding == null || embedding.length == 0) {
                throw new GlobalException(ErrorCode.CHALLENGE_CALCULATE_EMBEDDING);
            }

            if (embedding.length != EMBEDDING_DIM) {
                throw new GlobalException(ErrorCode.CHALLENGE_CALCULATE_EMBEDDING);
            }

            sanitizeEmbedding(embedding, challengeId);

            byte[] embeddingBytes = floatArrayToByteArray(embedding);

            Challenge challengeRef = challengeRepository.getReferenceById(challengeId);

            // 동시성에서 원자적이지 않아 unique 충돌 재발 가능한 문제 방어
            // 기존 레코드 조회 후 null이면 insert 시도, null 아니면 바로 update
            // insert 시 DataIntegrityViolationException 발생하면 재조회 후 update 재시도
            ChallengeEmbedding embeddingEntity = challengeEmbeddingRepository
                    .findByChallengeId(challengeId)
                    .orElse(null);

            if (embeddingEntity == null) {
                try {
                    embeddingEntity = ChallengeEmbedding.builder()
                            .challenge(challengeRef)
                            .challengeText(challengeText)
                            .challengeEmbedding(embeddingBytes)
                            .build();
                    challengeEmbeddingRepository.save(embeddingEntity);
                } catch (DataIntegrityViolationException e) {
                    embeddingEntity = challengeEmbeddingRepository
                            .findByChallengeId(challengeId)
                            .orElseThrow(() -> e);
                    embeddingEntity.setChallengeText(challengeText);
                    embeddingEntity.setChallengeEmbedding(embeddingBytes);
                }
            } else {
                embeddingEntity.setChallengeText(challengeText);
                embeddingEntity.setChallengeEmbedding(embeddingBytes);
            }

            challengeEmbeddingRepository.save(embeddingEntity);

        } catch (Exception e) {
            log.warn("임베딩 계산 실패. challengeId={}, error={}", challengeId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 모든 재시도가 실패했을 때 호출할 "0벡터 fallback 저장"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveZeroEmbeddingFallback(Long challengeId, String challengeText) {
        float[] zeros = new float[EMBEDDING_DIM]; // 기본 0.0f로 채워짐
        byte[] embeddingBytes = floatArrayToByteArray(zeros);

        Challenge challengeRef = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 기존 row 업데이트
        ChallengeEmbedding entity = challengeEmbeddingRepository.findByChallengeId(challengeId)
                .orElseGet(() -> ChallengeEmbedding.builder()
                        .challenge(challengeRef)
                        .build()
                );

        entity.setChallengeText(challengeText);
        entity.setChallengeEmbedding(embeddingBytes);

        challengeEmbeddingRepository.save(entity);

        log.warn(
                "[Fallback Saved] 768 길이 0벡터로 저장했습니다. 수동으로 임베딩 값을 수정해주세요. challengeId={}",
                challengeId
        );

    }

    private void sanitizeEmbedding(float[] embedding, Long challengeId) {
        int fixedCount = 0;
        for (int i = 0; i < embedding.length; i++) {
            float v = embedding[i];
            if (Float.isNaN(v) || Float.isInfinite(v)) {
                embedding[i] = 0.0f;
                fixedCount++;
            }
        }
        if (fixedCount > 0) {
            log.warn("[Embedding] NaN/Inf {}개를 0.0f로 치환했습니다. challengeId={}", fixedCount, challengeId);
        }
    }

    private byte[] floatArrayToByteArray(float[] floats) {
        int length = floats.length;
        byte[] bytes = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            int bits = Float.floatToIntBits(floats[i]);
            bytes[i * 4]     = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) (bits);
        }
        return bytes;
    }
}
