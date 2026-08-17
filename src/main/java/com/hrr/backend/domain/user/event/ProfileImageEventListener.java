package com.hrr.backend.domain.user.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.hrr.backend.global.s3.S3Service;

import lombok.RequiredArgsConstructor;

/**
 * 프로필 이미지 삭제 이벤트 리스너
 * 트랜잭션 커밋 후 S3에서 기존 이미지를 삭제합니다.
 */
@Component
@RequiredArgsConstructor
public class ProfileImageEventListener {

    private final S3Service s3Service;

    /**
     * 트랜잭션 커밋 후 기존 프로필 이미지 삭제
     * @param event 프로필 이미지 삭제 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProfileImageDeleted(ProfileImageDeletedEvent event) {
        String oldImageKey = event.getOldImageKey();

        if (oldImageKey != null && !oldImageKey.isBlank()) {
            s3Service.deleteFileByKey(oldImageKey);
        }
    }
}
