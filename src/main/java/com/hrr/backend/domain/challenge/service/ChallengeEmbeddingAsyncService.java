package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeEmbedding;
import com.hrr.backend.domain.challenge.repository.ChallengeEmbeddingRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeEmbeddingAsyncService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeEmbeddingRepository challengeEmbeddingRepository;
    private final EmbeddingClient embeddingClient;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateAndSaveEmbeddingAsync(Long challengeId, String challengeText) {
        try {
            // 임베딩 계산
            float[] embedding = embeddingClient.getEmbedding(challengeText);
            byte[] embeddingBytes = floatArrayToByteArray(embedding);

            Challenge challengeRef = challengeRepository.getReferenceById(challengeId);

            ChallengeEmbedding embeddingEntity = ChallengeEmbedding.builder()
                    .challenge(challengeRef)
                    .challengeText(challengeText)
                    .challengeEmbedding(embeddingBytes)
                    .build();

            challengeEmbeddingRepository.save(embeddingEntity);

        } catch (Exception e) {
            log.error("예상치 못한 오류로 임베딩 계산 실패. challengeId={}", challengeId, e);
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
