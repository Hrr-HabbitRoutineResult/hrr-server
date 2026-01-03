package com.hrr.backend.domain.user.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로필 이미지 삭제 이벤트
 * 트랜잭션 커밋 후 S3에서 기존 이미지를 삭제하기 위한 이벤트
 */
@Getter
@RequiredArgsConstructor
public class ProfileImageDeletedEvent {
    private final String oldImageKey;
}