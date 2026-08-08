package com.hrr.backend.domain.fcm.service;

import com.hrr.backend.domain.fcm.converter.FcmConverter;
import com.hrr.backend.domain.fcm.dto.FcmRequest;
import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FcmServiceImpl implements FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void registerFcmToken(FcmRequest.RegisterDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        fcmTokenRepository.findByUserAndToken(user, request.getFcmToken())
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.activateToken();
                            }
                        },
                        () -> fcmTokenRepository.save(FcmConverter.toEntity(request, user))
                );

        log.info("[registerFcmToken] FCM token 등록 상태를 동기화했습니다. userId={}", user.getId());

    }

    @Override
    @Transactional
    public void unregisterFcmToken(FcmRequest.UnregisterDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        FcmToken token = fcmTokenRepository.findByUserAndToken(user, request.getFcmToken())
                .orElseThrow(() -> new GlobalException(ErrorCode.FCM_TOKEN_NOT_FOUND)); // 존재하지 않으면 예외

        if (token.isActive()) {
            token.deactivateToken();
        }
        log.info("[unregisterFcmToken] FCM token 비활성화를 완료했습니다. userId={}", user.getId());
    }

}
