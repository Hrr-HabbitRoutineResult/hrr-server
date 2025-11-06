package com.hrr.backend.domain.fcm.converter;

import com.hrr.backend.domain.fcm.dto.FcmRequest;
import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.user.entity.User;

import java.time.LocalDateTime;

public class FcmConverter {

    //FCM 등록 요청 DTO → Entity 변환
    public static FcmToken toEntity(FcmRequest.RegisterDto request, User user) {
        return FcmToken.builder()
                .user(user)
                .token(request.getFcmToken())
                .registeredAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }
}
