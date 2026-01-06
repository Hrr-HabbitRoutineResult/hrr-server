package com.hrr.backend.domain.challenge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeStatics;
import com.hrr.backend.domain.challenge.repository.ChallengeStaticsRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.common.enums.AgeGroup;
import com.hrr.backend.global.common.enums.AvailableTime;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.FavorType;
import com.hrr.backend.global.common.enums.Gender;
import com.hrr.backend.global.common.enums.Goal;
import com.hrr.backend.global.common.enums.Job;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeStaticsServiceImpl implements ChallengeStaticsService {

    private final ChallengeStaticsRepository staticsRepository;
    private final UserChallengeRepository userChallengeRepository;

    /**
     * 특정 챌린지의 모든 선호 타입에 대한 기본 통계 데이터를 생성합니다.
     */
    @Transactional
    @Override
    public void createInitialStatics(Challenge challenge) {
        List<ChallengeStatics> initialStaticsList = new ArrayList<>();

        for (FavorType type : FavorType.values()) {
            List<String> values = getValuesForType(type);

            for (String value : values) {
                initialStaticsList.add(
                        ChallengeStatics.builder()
                                .challenge(challenge)
                                .favorType(type)
                                .favorValue(value)
                                .number(0)
                                .build()
                );
            }
        }

        staticsRepository.saveAll(initialStaticsList);
    }

    /**
     * 특정 챌린지의 통계 데이터의 수치를 업데이트 합니다.
     * (전제: User 1명당 UserFavor 1개)
     */
    @Transactional
    @Override
    public void updateChallengeStatics(Challenge challenge) {

        // 해당 챌린지의 모든 참여 정보 조회
        List<UserChallenge> participation =
                userChallengeRepository.findAllJoinedWithUserFavorByChallengeId(challenge.getId());

        if (participation.isEmpty()) return;

        // 집계를 위한 임시 맵 (FavorType별로 상세 값과 카운트를 저장)
        Map<FavorType, Map<String, Integer>> aggregateMap = new HashMap<>();

        for (UserChallenge pc : participation) {
            User user = pc.getUser();
            if (user == null) continue;

            // 유저당 1개 선호 정보만 사용
            UserFavor favor = user.getUserFavor();
            if (favor == null) continue;

            // 단일 선택 항목들 집계
            if (favor.getGender() != null) {
                accumulate(aggregateMap, FavorType.GENDER, favor.getGender().name());
            }
            if (favor.getAgeGroup() != null) {
                accumulate(aggregateMap, FavorType.AGE_GROUP, favor.getAgeGroup().name());
            }
            if (favor.getJob() != null) {
                accumulate(aggregateMap, FavorType.JOB, favor.getJob().name());
            }
            if (favor.getGoal() != null) {
                accumulate(aggregateMap, FavorType.GOAL, favor.getGoal().name());
            }

            // 다중 선택 항목 (AvailableTime) 집계
            if (favor.getAvailableTime() != null && !favor.getAvailableTime().isEmpty()) {
                favor.getAvailableTime().forEach(time ->
                        accumulate(aggregateMap, FavorType.AVAILABLE_TIME, time.name())
                );
            }

            // 다중 선택 항목 (Category) 집계
            if (favor.getCategory() != null && !favor.getCategory().isEmpty()) {
                favor.getCategory().forEach(cat ->
                        accumulate(aggregateMap, FavorType.CATEGORY, cat.name())
                );
            }
        }

        // 집계된 데이터를 ChallengeStatics 테이블에 최종 반영
        applyAggregatedData(challenge, aggregateMap);
    }

    /**
     * 특정 선호 타입과 그 종류에 대한 통계를 조회하고 없으면 기본 테이블 생성 후 해당하는 데이터만 반환
     */
    @Transactional
    @Override
    public ChallengeStatics getOrUpdateStatics(Challenge challenge, FavorType type, String value) {
        return staticsRepository.findByChallengeAndFavorTypeAndFavorValue(challenge, type, value)
                .orElseGet(() -> {
                    createInitialStatics(challenge);
                    return staticsRepository.findByChallengeAndFavorTypeAndFavorValue(challenge, type, value)
                            .orElseThrow(() -> new GlobalException(ErrorCode.STATICS_NOT_FOUND));
                });
    }

    /**
     * 선호 타입에 해당하는 옵션들을 불러오는 메서드
     */
    private List<String> getValuesForType(FavorType type) {
        return switch (type) {
            case GENDER -> Arrays.stream(Gender.values()).map(Enum::name).toList();
            case AGE_GROUP -> Arrays.stream(AgeGroup.values()).map(Enum::name).toList();
            case JOB -> Arrays.stream(Job.values()).map(Enum::name).toList();
            case AVAILABLE_TIME -> Arrays.stream(AvailableTime.values()).map(Enum::name).toList();
            case CATEGORY -> Arrays.stream(Category.values()).map(Enum::name).toList();
            case GOAL -> Arrays.stream(Goal.values()).map(Enum::name).toList();
            default -> Collections.emptyList();
        };
    }

    // 맵에 숫자를 누적시키는 헬퍼 메서드
    private void accumulate(Map<FavorType, Map<String, Integer>> counts, FavorType type, String value) {
        counts.computeIfAbsent(type, k -> new HashMap<>())
                .merge(value, 1, Integer::sum);
    }

    private int pct(int count, int total) {
        if (total <= 0) return 0;
        return (int) Math.round((count * 100.0) / total);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<FavorType, Map<String, Integer>>> loadStaticsCountMap(List<Long> challengeIds) {
        List<ChallengeStatics> rows = staticsRepository.findByChallengeIdIn(challengeIds);

        Map<Long, Map<FavorType, Map<String, Integer>>> result = new HashMap<>();

        for (ChallengeStatics cs : rows) {
            Long chId = cs.getChallenge().getId();
            result.computeIfAbsent(chId, k -> new HashMap<>())
                    .computeIfAbsent(cs.getFavorType(), k -> new HashMap<>())
                    .put(cs.getFavorValue(), cs.getNumber());
        }
        return result;
    }
    public void applyStaticsToItems(List<ChallengeItemDto> items) {
        List<Long> ids = items.stream().map(ChallengeItemDto::getChallengeId).toList();
        Map<Long, Map<FavorType, Map<String, Integer>>> staticsMap = loadStaticsCountMap(ids);

        for (ChallengeItemDto item : items) {
            Map<FavorType, Map<String, Integer>> byType = staticsMap.get(item.getChallengeId());
            if (byType == null) {
                // 통계 없으면 0으로
                setAllPctZero(item);
                continue;
            }

            int male = getCount(byType, FavorType.GENDER, Gender.MALE.name());
            int female = getCount(byType, FavorType.GENDER, Gender.FEMALE.name());
            int total = male + female;

            item.setParticipants_male_pct(pct(male, total));
            item.setParticipants_female_pct(pct(female, total));

            int a10 = getCount(byType, FavorType.AGE_GROUP, AgeGroup.TEENS.name());
            int a20 = getCount(byType, FavorType.AGE_GROUP, AgeGroup.TWENTIES.name());
            int a30 = getCount(byType, FavorType.AGE_GROUP, AgeGroup.THIRTIES.name());
            int a40 = getCount(byType, FavorType.AGE_GROUP, AgeGroup.FORTIES.name());
            int a50 = getCount(byType, FavorType.AGE_GROUP, AgeGroup.FIFTIES_PLUS.name());

            int ageTotal = a10 + a20 + a30 + a40 + a50;
            int ageBaseCount = (ageTotal > 0) ? ageTotal : total;

            item.setAge_10s_pct(pct(a10, ageBaseCount));
            item.setAge_20s_pct(pct(a20, ageBaseCount));
            item.setAge_30s_pct(pct(a30, ageBaseCount));
            item.setAge_40s_pct(pct(a40, ageBaseCount));
            item.setAge_50p_pct(pct(a50, ageBaseCount));

            int jm = getCount(byType, FavorType.JOB, Job.STUDENT_MIDDLE_HIGH.name());
            int ju = getCount(byType, FavorType.JOB, Job.STUDENT_UNIVERSITY.name());
            int jj = getCount(byType, FavorType.JOB, Job.JOB_SEEKER.name());
            int je = getCount(byType, FavorType.JOB, Job.EMPLOYEE.name());
            int jh = getCount(byType, FavorType.JOB, Job.HOMEMAKER.name());
            int jc = getCount(byType, FavorType.JOB, Job.ETC.name());

            int jobTotal = jm + ju + jj + je + jh + jc;
            int jobBaseCount = (jobTotal > 0) ? jobTotal : total;

            item.setJob_student_middle_high_pct(pct(jm, jobBaseCount));
            item.setJob_student_university_pct(pct(ju, jobBaseCount));
            item.setJob_job_seeker_pct(pct(jj, jobBaseCount));
            item.setJob_employee_pct(pct(je, jobBaseCount));
            item.setJob_homemaker_pct(pct(jh, jobBaseCount));
            item.setJob_etc_pct(pct(jc, jobBaseCount));
        }
    }

    private int getCount(Map<FavorType, Map<String, Integer>> byType, FavorType type, String value) {
        Map<String, Integer> m = byType.get(type);
        if (m == null) return 0;
        return m.getOrDefault(value, 0);
    }

    private void setAllPctZero(ChallengeItemDto item) {
        item.setParticipants_male_pct(0);
        item.setParticipants_female_pct(0);

        item.setAge_10s_pct(0);
        item.setAge_20s_pct(0);
        item.setAge_30s_pct(0);
        item.setAge_40s_pct(0);
        item.setAge_50p_pct(0);

        item.setJob_student_middle_high_pct(0);
        item.setJob_student_university_pct(0);
        item.setJob_job_seeker_pct(0);
        item.setJob_employee_pct(0);
        item.setJob_homemaker_pct(0);
        item.setJob_etc_pct(0);
    }


    // 최종적으로 DB에 저장하는 메서드
    private void applyAggregatedData(Challenge challenge, Map<FavorType, Map<String, Integer>> aggregateMap) {

        // 해당 챌린지의 모든 기존 통계 데이터를 가져와서 수치를 0으로 초기화
        List<ChallengeStatics> allStatics = staticsRepository.findByChallenge(challenge);

        // 기존 데이터가 있을 때만 0으로 초기화
        allStatics.forEach(s -> s.updateNumber(0));

        // 집계된 결과 반영
        aggregateMap.forEach((type, valueMap) -> {
            valueMap.forEach((value, count) -> {
                ChallengeStatics statics = getOrUpdateStatics(challenge, type, value);
                statics.updateNumber(count);
            });
        });
    }
}
