package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long myId, Long targetUserId) {

        return userRepository.findUserProfileById(myId, targetUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}