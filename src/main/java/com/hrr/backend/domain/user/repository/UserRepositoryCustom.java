package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import java.util.Optional;

public interface UserRepositoryCustom {
    Optional<UserResponseDto> findUserProfileById(Long myId, Long targetUserId);
}