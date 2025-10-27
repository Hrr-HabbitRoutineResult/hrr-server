package com.hrr.backend.domain.fcm.service;

import com.hrr.backend.domain.fcm.dto.FcmRequest;

public interface FcmService {
    void registerFcmToken(FcmRequest.RegisterDto request);
    void unregisterFcmToken(FcmRequest.UnregisterDto request);
}
