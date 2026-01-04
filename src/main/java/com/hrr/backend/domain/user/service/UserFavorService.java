package com.hrr.backend.domain.user.service;

import java.util.LinkedHashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.repository.UserFavorRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserFavorService {

    private final UserFavorRepository userFavorRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserFavor saveUserFavor(ChallengeRecommendRequest request) {

        // 1. 유저 존재 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 2. 기존 UserFavor 조회 (있으면 UPDATE, 없으면 새로 생성)
        UserFavor userFavor = userFavorRepository.findByUserId(user.getId())
                .orElseGet(() -> UserFavor.builder()
                        .user(user)
                        .build());

        // 3. 요청 값으로 덮어쓰기 (업데이트)
        userFavor.setGender(request.getGender());
        userFavor.setAgeGroup(request.getAgeGroup());
        userFavor.setJob(request.getJob());
        userFavor.setGoal(request.getGoal());

        // List → Set (순서 유지 + 중복 제거)
        userFavor.setAvailableTime(
                request.getAvailableTime() == null
                        ? null
                        : new LinkedHashSet<>(request.getAvailableTime())
        );

        userFavor.setCategory(
                request.getCategory() == null
                        ? null
                        : new LinkedHashSet<>(request.getCategory())
        );

        // 4. save → 기존이면 UPDATE, 없으면 INSERT
        return userFavorRepository.save(userFavor);
    }
}
