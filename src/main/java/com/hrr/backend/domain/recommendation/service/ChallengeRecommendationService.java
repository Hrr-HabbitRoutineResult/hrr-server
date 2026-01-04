package com.hrr.backend.domain.recommendation.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemResponseDto;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeRecommendResult;
import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.domain.recommendation.repository.RecommendationRepository;
import com.hrr.backend.domain.recommendation.repository.RecommendationResultRepository;
import com.hrr.backend.domain.recommendation.entity.RecommendationResult;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.service.UserFavorService;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeRecommendationService {

    private final RecommendationRepository challengeRepository; // 메타 조회용 커스텀 repo
    private final RecommendationResultRepository recommendationResultRepository;
    private final ChallengeRepository challengeJpaRepository;   // Challenge 엔티티 조회용 JPA repo

    private final UserFavorService userFavorService;

    private final ModelApiClient modelApiClient;
    private final S3UrlUtil s3UrlUtil;

    private static final int EMBED_DIM = 768;

    @Transactional
    public ChallengeRecommendResult recommendChallenges(ChallengeRecommendRequest request) {

        // 0) 유저 검증 + 선호 저장
        UserFavor favor = userFavorService.saveUserFavor(request);

        // 1) 전체 챌린지 메타 조회
        List<ChallengeItemDto> allChallenges = challengeRepository.findAllChallengeMeta();
        if (allChallenges.isEmpty()) {
            log.warn("[Recommend] No challenges found in DB. Returning empty result.");
            return ChallengeRecommendResult.builder()
                    .userId(request.getUserId())
                    .recommendations(List.of())
                    .build();
        }

        // 2) 임베딩 검증
        for (ChallengeItemDto ch : allChallenges) {
            if (ch.getEmbedding() == null || ch.getEmbedding().size() != EMBED_DIM) {
                throw new GlobalException(ErrorCode.EMBEDDING_LENGTH_ERROR);
            }
        }

        // 3) cert_time_slots 세팅
        allChallenges.forEach(ch ->
                ch.setCert_time_slots(toAvailableTimeSlots(
                        ch.getVerifyStartTime(),
                        ch.getVerifyEndTime()
                ))
        );

        // 4) E5 query 생성 (요청 포맷 적용 + 카테고리 쉼표 나열)
        String query = buildUserQueryE5(favor);

        // 5) 모델 호출
        int topK = 5;
        ModelApiResponse modelApiResponse;
        try {
            modelApiResponse = modelApiClient.requestRecommendations(
                    ModelApiRequest.builder()
                            .query(query)
                            .items(allChallenges)
                            .topK(topK)
                            .build()
            );
            log.info("[Recommend] Model-api response: version={}, latencyMs={}",
                    modelApiResponse != null ? modelApiResponse.getModelVersion() : null,
                    modelApiResponse != null ? modelApiResponse.getLatencyMs() : null);
        } catch (Exception e) {
            log.error("[Recommend] Model API call failed", e);
            throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
        }

        // 6) 추천 결과 저장
        saveRecommendationResults(favor, modelApiResponse);

        // 7) 응답 매핑
        return mapToResultDto(request.getUserId(), allChallenges, modelApiResponse);
    }

    /* ======================= helpers ======================= */
    private String buildUserQueryE5(UserFavor favor) {
        String goalText = (favor.getGoal() == null)
                ? "원하는"
                : favor.getGoal().getDescription();

        String categoryText = (favor.getCategory() == null || favor.getCategory().isEmpty())
                ? "전체"
                : favor.getCategory().stream()
                .map(Category::getDescription)
                .collect(Collectors.joining(", ")); // ✅ 쉼표 나열

        return String.format("query: %s 목표를 위한 %s 챌린지", goalText, categoryText);
    }

    private void saveRecommendationResults(UserFavor favor, ModelApiResponse modelApiResponse) {
        if (modelApiResponse == null || modelApiResponse.getRecommendations() == null) return;

        int rank = 1;
        List<RecommendationResult> results = new ArrayList<>();

        for (var rec : modelApiResponse.getRecommendations()) {
            Challenge challenge = challengeJpaRepository
                    .findById(rec.getChallengeId())
                    .orElse(null);
            if (challenge == null) continue;

            results.add(
                    RecommendationResult.builder()
                            .userFavor(favor)
                            .challenge(challenge)
                            .cosineScore((float) rec.getMatchScore())
                            .ranking(rank++)
                            .build()
            );
        }

        recommendationResultRepository.saveAll(results);
    }

    private ChallengeRecommendResult mapToResultDto(
            Long userId,
            List<ChallengeItemDto> allChallenges,
            ModelApiResponse modelApiResponse
    ) {
        if (modelApiResponse == null || modelApiResponse.getRecommendations() == null) {
            return ChallengeRecommendResult.builder()
                    .userId(userId)
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
                            if (base == null) return null;

                            return ChallengeItemResponseDto.builder()
                                    .challengeId(base.getChallengeId())
                                    .title(base.getTitle())
                                    .description(base.getDescription())
                                    .category(base.getCategory())
                                    .certTimeSlots(base.getCert_time_slots())
                                    .goalText(base.getGoal_text())
                                    .verifyStartTime(base.getVerifyStartTime())
                                    .verifyEndTime(base.getVerifyEndTime())
                                    .imageKey(s3UrlUtil.toFullUrl(base.getImageKey()))
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList();

        return ChallengeRecommendResult.builder()
                .userId(userId)
                .modelVersion(modelApiResponse.getModelVersion())
                .latencyMs(modelApiResponse.getLatencyMs())
                .recommendations(items)
                .build();
    }

    /* ======================= time slot ======================= */

    private String toAvailableTimeSlots(LocalTime start, LocalTime end) {
        if (start == null || end == null) return null;

        List<String> slots = new ArrayList<>();
        if (start.getHour() < 5 || end.getHour() <= 5) slots.add("LATE_NIGHT");
        if (overlap(start, end, 5, 9)) slots.add("EARLY_MORNING");
        if (overlap(start, end, 9, 12)) slots.add("MORNING");
        if (overlap(start, end, 12, 14)) slots.add("LUNCH");
        if (overlap(start, end, 14, 18)) slots.add("AFTERNOON");
        if (overlap(start, end, 18, 21)) slots.add("EVENING");
        if (overlap(start, end, 21, 24)) slots.add("NIGHT");

        return String.join(";", new LinkedHashSet<>(slots));
    }

    private boolean overlap(LocalTime s, LocalTime e, int a, int b) {
        return !(e.getHour() <= a || s.getHour() >= b);
    }
}
