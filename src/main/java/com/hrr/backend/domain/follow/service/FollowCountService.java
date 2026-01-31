package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowCountService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public void syncCounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        long followerCount = followRepository.countActiveFollowers(userId);
        long followingCount = followRepository.countActiveFollowings(userId);

        user.updateFollowCounts(followerCount, followingCount);
    }
}
