package com.hrr.backend.domain.recommendation.service;

import com.hrr.backend.domain.recommendation.repository.RecommendationRepository;
import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemResponseDto;
import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeRecommendResult;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRecommendationService {

    private final RecommendationRepository challengeRepository;
    private final ModelApiClient modelApiClient;

    private static final int EMBED_DIM = 768;

    public ChallengeRecommendResult recommendChallenges(ChallengeRecommendRequest request) {

        // 1) 전체 챌린지 메타 정보 조회
        List<ChallengeItemDto> allChallenges = challengeRepository.findAllChallengeMeta();

        if (allChallenges.isEmpty()) {
            log.warn("[Recommend] No challenges found in DB. Returning empty result.");
            return ChallengeRecommendResult.builder()
                    .userId(request.getUserId())
                    .modelVersion(null)
                    .latencyMs(0)
                    .recommendations(List.of())
                    .build();
        }

        boolean hasInvalidEmbedding = false;

        for (ChallengeItemDto ch : allChallenges) {
            List<Float> emb = ch.getEmbedding();
            int size = (emb == null) ? -1 : emb.size();

            if (emb == null || size != EMBED_DIM) {
                hasInvalidEmbedding = true;
            }
        }

        if (hasInvalidEmbedding) {
            throw new GlobalException(ErrorCode.EMBEDDING_LENGTH_ERROR);
        }

        allChallenges.forEach(ch -> {
            String slot = formatCertTimeSlots(ch.getVerifyStartTime(), ch.getVerifyEndTime());
            ch.setCert_time_slots(slot);
        });

        // 2) 모델에 보낼 query 문자열 구성
        String query = buildUserQuery(request);

        // 3) 모델 API 요청 DTO 생성
        ModelApiRequest modelApiRequest = ModelApiRequest.builder()
                .query(query)
                .items(allChallenges)
                .top_k(5)
                .build();

        ModelApiResponse modelApiResponse = null;
        try {
            // 4) 모델 서버 호출
            modelApiResponse = modelApiClient.requestRecommendations(modelApiRequest);

            log.info("[Recommend] Model-api response: version={}, latencyMs={}",
                    modelApiResponse != null ? modelApiResponse.getModelVersion() : null,
                    modelApiResponse != null ? modelApiResponse.getLatencyMs() : null);

        } catch (Exception e) {
            throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
        }

        // 5) 추천 결과를 최종 응답 DTO로 매핑
        return mapToResultDto(request, allChallenges, modelApiResponse);
    }

    private String formatCertTimeSlots(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return String.format("%02d-%02d", start.getHour(), end.getHour());
    }

    private String buildUserQuery(ChallengeRecommendRequest request) {
        String categories = Optional.ofNullable(request.getCategory())
                .orElse(List.of())
                .stream()
                .map(Enum::name)
                .collect(Collectors.joining("/"));

        return String.format(
                "성별: %s, 연령대: %s, 직업: %s, 활동 시간: %s, 관심사: %s, 목표: %s",
                request.getGender(),
                request.getAgeGroup(),
                request.getJob(),
                request.getAvailableTime(),
                categories,
                request.getGoal()
        );
    }

    private ChallengeRecommendResult mapToResultDto(
            ChallengeRecommendRequest request,
            List<ChallengeItemDto> allChallenges,
            ModelApiResponse modelApiResponse
    ) {
        if (modelApiResponse == null || modelApiResponse.getRecommendations() == null) {
            return ChallengeRecommendResult.builder()
                    .userId(request.getUserId())
                    .modelVersion(null)
                    .latencyMs(0)
                    .recommendations(List.of())
                    .build();
        }

        Map<Long, ChallengeItemDto> challengeMap = allChallenges.stream()
                .collect(Collectors.toMap(
                        ChallengeItemDto::getChallengeId,
                        Function.identity()
                ));

        List<ChallengeItemResponseDto> items =
                modelApiResponse.getRecommendations().stream()
                        .map(rec -> {
                            ChallengeItemDto base = challengeMap.get(rec.getChallengeId());
                            if (base == null) {
                                log.warn("[Recommend] Recommendation refers to unknown challengeId={}", rec.getChallengeId());
                            }

                            return ChallengeItemResponseDto.builder()
                                    .challengeId(base.getChallengeId())
                                    .title(base.getTitle())
                                    .description(base.getDescription())
                                    .category(base.getCategory())
                                    .cert_time_slots(base.getCert_time_slots())
                                    .goal_text(base.getGoal_text())
                                    .verifyStartTime(base.getVerifyStartTime())
                                    .verifyEndTime(base.getVerifyEndTime())
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList();


        return ChallengeRecommendResult.builder()
                .userId(request.getUserId())
                .modelVersion(modelApiResponse.getModelVersion())
                .latencyMs(modelApiResponse.getLatencyMs())
                .recommendations(items)
                .build();
    }
}
