package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String rawNickname) {
        String nickname = normalize(rawNickname);
        if (nickname.isEmpty()) {
            throw new GlobalException(ErrorCode.NICKNAME_BLANK);
        }

        if (nickname.length() > 10) {
            throw new GlobalException(ErrorCode.NICKNAME_TOO_LONG);
        }

        return !userRepository.existsByNickname(nickname);
    }

    @Override
    public UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request) {
        String nickname = normalize(request.getNickname());

        if (nickname.isEmpty()) {
            throw new GlobalException(ErrorCode.NICKNAME_BLANK);
        }

        if (nickname.length() > 10) {
            throw new GlobalException(ErrorCode.NICKNAME_TOO_LONG);
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 닉네임 저장 + 로그인 상태 ACTIVE 전환
        user.updateNickname(nickname);
        user.updateLoginStatus(LoginStatus.ACTIVE);

        // 변경내용 반영
        userRepository.save(user);

        return UserNicknameResponseDto.builder()
                .nickname(nickname)
                .message("사용 가능한 닉네임이에요.")
                .nextStep(user.determineNextStep())   // User 엔티티에 이미 있는 메서드 사용
                .build();
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }
}
