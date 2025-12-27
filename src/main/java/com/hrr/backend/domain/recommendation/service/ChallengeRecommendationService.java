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
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.repository.UserFavorRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
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

    private final RecommendationRepository challengeRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final UserRepository userRepository;
    private final UserFavorRepository userFavorRepository;
    private final ChallengeRepository challengeJpaRepository;

    private final ModelApiClient modelApiClient;
    private final S3UrlUtil s3UrlUtil;

    private static final int EMBED_DIM = 768;

    @Transactional
    public ChallengeRecommendResult recommendChallenges(ChallengeRecommendRequest request) {

        // 0) 유저 검증
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 1) UserFavor로 저장
        UserFavor favor = UserFavor.builder()
                .user(user)
                .gender(request.getGender())
                .ageGroup(request.getAgeGroup())
                .job(request.getJob())
                .availableTime(new LinkedHashSet<>(request.getAvailableTime()))
                .category(new LinkedHashSet<>(request.getCategory()))
                .goal(request.getGoal())
                .build();

        favor = userFavorRepository.save(favor);

        // 2) 전체 챌린지 메타 조회
        List<ChallengeItemDto> allChallenges = challengeRepository.findAllChallengeMeta();
        if (allChallenges.isEmpty()) {
            log.warn("[Recommend] No challenges found in DB. Returning empty result.");
            return ChallengeRecommendResult.builder()
                    .userId(request.getUserId())
                    .recommendations(List.of())
                    .build();
        }

        // 3) 임베딩 검증
        for (ChallengeItemDto ch : allChallenges) {
            if (ch.getEmbedding() == null || ch.getEmbedding().size() != EMBED_DIM) {
                throw new GlobalException(ErrorCode.EMBEDDING_LENGTH_ERROR);
            }
        }

        // 4) cert_time_slots → "EVENING;NIGHT" 형태
        allChallenges.forEach(ch ->
                ch.setCert_time_slots(toAvailableTimeSlots(
                        ch.getVerifyStartTime(),
                        ch.getVerifyEndTime()
                ))
        );

        // 5) E5 query 생성
        String query = buildUserQueryE5(favor);

        // 6) 모델 호출
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

        // 7) 추천 결과 저장 (RecommendationResult)
        saveRecommendationResults(favor, modelApiResponse);

        // 8) 응답 매핑
        return mapToResultDto(request.getUserId(), allChallenges, modelApiResponse);
    }

    /* ======================= helpers ======================= */

    private String buildUserQueryE5(UserFavor favor) {
        String timeKo = favor.getAvailableTime().isEmpty()
                ? "항상"
                : favor.getAvailableTime().stream()
                .map(t -> t.name() + "(" + t.getDescription() + ")")
                .collect(Collectors.joining("/"));

        String catKo = favor.getCategory().isEmpty()
                ? "전체"
                : favor.getCategory().stream()
                .map(Category::getDescription)
                .collect(Collectors.joining("/"));

        return String.format(
                "query: %s %s %s입니다.\n%s에 할 수 있는 %s 관련 챌린지를 찾고 있어요.\n목표는 %s입니다.",
                favor.getAgeGroup().getDescription(),
                favor.getGender().getDescription(),
                favor.getJob().getDescription(),
                timeKo,
                catKo,
                favor.getGoal().getDescription()
        );
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
