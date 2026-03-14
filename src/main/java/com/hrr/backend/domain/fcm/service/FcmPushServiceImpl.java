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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushServiceImpl implements FcmPushService {

    private static final int FCM_MULTICAST_LIMIT = 500;

    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Override
    public void sendPushForDeliveries(List<NotificationDelivery> deliveries, NotificationEvent event) {
        if (deliveries.isEmpty()) return;

        boolean isMandatory = event.getType().isMandatory();
        List<User> receivers = deliveries.stream()
                .map(NotificationDelivery::getReceiver)
                .toList();

        // 알림 설정을 IN절로 한 번에 조회 → userId 기준 Map으로 변환
        Map<Long, NotificationSetting> settingMap = notificationSettingRepository
                .findAllByUserIn(receivers)
                .stream()
                .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s));

        // 발송 대상 유저 필터링
        // isMandatory=true(필수 알림)면 유저 설정과 무관하게 발송, 설정 레코드가 없으면 기본 허용
        List<User> eligibleReceivers = receivers.stream()
                .filter(user -> {
                    if (isMandatory) return true;
                    NotificationSetting setting = settingMap.get(user.getId());
                    if (setting == null) return true;
                    return isCategoryEnabled(setting, event.getCategory());
                })
                .toList();

        if (eligibleReceivers.isEmpty()) {
            log.debug("발송 대상 없음: category={}, mandatory={}", event.getCategory(), isMandatory);
            return;
        }

        // 활성 토큰을 IN절로 한 번에 조회
        List<String> allTokens = fcmTokenRepository.findAllActiveTokensByUsers(eligibleReceivers);

        if (allTokens.isEmpty()) {
            log.debug("활성 FCM 토큰 없음: 대상={}명", eligibleReceivers.size());
            return;
        }

        // FCM 500개 제한에 맞춰 배치 분할 후 벌크 전송
        List<List<String>> batches = partitionTokens(allTokens, FCM_MULTICAST_LIMIT);
        for (List<String> batch : batches) {
            sendMulticast(batch, event, eligibleReceivers.size());
        }
    }

    @Override
    public void sendPushForDelivery(NotificationDelivery delivery, NotificationEvent event) {
        sendPushForDeliveries(List.of(delivery), event);
    }

    private boolean isCategoryEnabled(NotificationSetting setting, NotificationCategory category) {
        if (setting.isAllPaused()) return false;
        return switch (category) {
            case CHALLENGE -> setting.isChallengeEnabled();
            case VERIFICATION -> setting.isVerificationEnabled();
            case FOLLOW -> setting.isFollowEnabled();
            case BADGE -> setting.isBadgeEnabled();
        };
    }

    /** 토큰 리스트를 maxSize 단위로 분할 (FCM 500개 제한 대응) */
    private List<List<String>> partitionTokens(List<String> tokens, int maxSize) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += maxSize) {
            partitions.add(tokens.subList(i, Math.min(i + maxSize, tokens.size())));
        }
        return partitions;
    }

    private void sendMulticast(List<String> tokens, NotificationEvent event, int totalReceivers) {
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

            log.info("FCM 벌크 발송 완료: 대상={}명, 토큰={}개, 성공={}, 실패={}",
                    totalReceivers, tokens.size(), response.getSuccessCount(), response.getFailureCount());

            handleFailedTokens(tokens, response);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 벌크 발송 실패: 토큰={}개, error={}", tokens.size(), e.getMessage(), e);
        }
    }

    private void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();

                // 토큰이 유효하지 않거나 만료된 것이 확실한 경우만 비활성화
                if (errorCode == MessagingErrorCode.UNREGISTERED) {
                    String failedToken = tokens.get(i);
                    fcmTokenRepository.deactivateAllByToken(failedToken);
                    log.info("유효하지 않은 FCM 토큰 전체 비활성화 완료: token={}...",
                            failedToken.substring(0, Math.min(20, failedToken.length())));
                }
                // 메시지 페이로드 오류 가능성이 있는 경우 로그만 출력 (비활성화 X)
                else if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.warn("FCM 메시지 페이로드 또는 토큰 형식이 잘못되었습니다. (INVALID_ARGUMENT): token={}, message={}",
                            tokens.get(i), sendResponse.getException().getMessage());
                }
            }
        }
    }
}