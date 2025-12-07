package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserVerificationResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.user.repository.UserVerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserVerificationServiceImpl implements UserVerificationService {

    private final UserRepository userRepository;
    private final UserVerificationRepository userVerificationRepository;

    @Override
    public SliceResponseDto<UserVerificationResponseDto.VerificationItemDto> getVerificationHistory(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 인증 기록 조회
        Slice<UserVerificationResponseDto.VerificationItemDto> slice =
                userVerificationRepository.findVerificationHistoryByUser(user, pageable);

        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(slice);
    }
}