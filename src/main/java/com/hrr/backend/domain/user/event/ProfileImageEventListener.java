package com.hrr.backend.domain.user.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import com.hrr.backend.global.s3.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 프로필 이미지 삭제 이벤트 리스너
 * 트랜잭션 커밋 후 S3에서 기존 이미지를 삭제합니다.
 */
@Slf4j
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
            log.info("프로필 이미지 삭제 시작 (트랜잭션 커밋 후): {}", oldImageKey);
            s3Service.deleteFileByKey(oldImageKey);
            log.info("프로필 이미지 삭제 완료: {}", oldImageKey);
        }
    }
}