package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushServiceImpl implements FcmPushService {

    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public void sendPushForDeliveries(List<NotificationDelivery> deliveries, NotificationEvent event) {
        for (NotificationDelivery delivery : deliveries) {
            User receiver = delivery.getReceiver();

            // 유저 알림 설정 조회 (설정 없으면 발송 허용)
            boolean allowed = notificationSettingRepository.findByUser(receiver)
                    .map(setting -> isCategoryEnabled(setting, event.getCategory()))
                    .orElse(true);

            if (!allowed) {
                log.debug("알림 설정으로 인해 푸시 발송 건너뜀: userId={}, category={}", receiver.getId(), event.getCategory());
                continue;
            }

            // 해당 유저의 활성 FCM 토큰 목록 조회
            List<String> tokens = fcmTokenRepository.findAllActiveTokensByUser(receiver);

            if (tokens.isEmpty()) {
                log.debug("활성 FCM 토큰 없음: userId={}", receiver.getId());
                continue;
            }

            sendMulticast(tokens, event, receiver.getId());
        }
    }

    @Override
    public void sendPushForDelivery(NotificationDelivery delivery, NotificationEvent event) {
        sendPushForDeliveries(List.of(delivery), event);
    }

    private boolean isCategoryEnabled(NotificationSetting setting, NotificationCategory category) {
        if (setting.isAllPaused()) {
            return false;
        }
        return switch (category) {
            case CHALLENGE -> setting.isChallengeEnabled();
            case VERIFICATION -> setting.isVerificationEnabled();
            case FOLLOW -> setting.isFollowEnabled();
            case BADGE -> setting.isBadgeEnabled();
        };
    }

    private void sendMulticast(List<String> tokens, NotificationEvent event, Long receiverUserId) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(
                        Notification.builder()
                                .setTitle(event.getTitle())
                                .setBody(event.getMessage())
                                .setImage(event.getImageKey())
                                .build()
                )
                .putData("targetType", event.getTargetType() != null ? event.getTargetType().name() : "")
                .putData("targetId", event.getTargetId() != null ? String.valueOf(event.getTargetId()) : "")
                .putData("contextType", event.getContextType() != null ? event.getContextType().name() : "")
                .putData("contextId", event.getContextId() != null ? String.valueOf(event.getContextId()) : "")
                .putData("category", event.getCategory() != null ? event.getCategory().name() : "")
                .putData("typeName", event.getType() != null ? event.getType().getTypeName().name() : "")
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            log.info("FCM 멀티캐스트 발송 완료: userId={}, 성공={}, 실패={}",
                    receiverUserId, response.getSuccessCount(), response.getFailureCount());

            handleFailedTokens(tokens, response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 멀티캐스트 발송 실패: userId={}, error={}", receiverUserId, e.getMessage(), e);
        }
    }

    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();

                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    String failedToken = tokens.get(i);
                    fcmTokenRepository.findByToken(failedToken)
                            .ifPresent(fcmToken -> {
                                fcmToken.deactivateToken();
                                fcmTokenRepository.save(fcmToken);
                                log.info("유효하지 않은 FCM 토큰 비활성화: token={}...",
                                        failedToken.substring(0, Math.min(20, failedToken.length())));
                            });
                }
            }
        }
    }
}