package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushServiceImpl implements FcmPushService {

    private static final int FCM_MULTICAST_LIMIT = 500;

    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final S3UrlUtil s3UrlUtil;
    private final FcmTokenDeactivationService fcmTokenDeactivationService;

    @Override
    public void sendPushForDeliveries(List<NotificationDelivery> deliveries, NotificationEvent event) {
        if (deliveries == null || deliveries.isEmpty()) return;

        // 필수 필드(Event, Type)에 대한 조기 리턴
        if (event == null || event.getType() == null) {
            log.warn("[sendPushForDeliveries] FCM 발송 대상 NotificationEvent 또는 NotificationType이 null입니다.");
            return;
        }

        boolean isMandatory = event.getType().isMandatory();
        NotificationCategory category = event.getCategory();

        // 카테고리 체크를 스트림 외부로 이동하여 로그 폭증 방지
        if (!isMandatory && category == null) {
            log.warn("[sendPushForDeliveries] 필수 알림이 아닌 FCM 발송 대상의 category가 null입니다.");
            return;
        }

        // 수신자 리스트 추출 및 ID 기반 중복 제거
        List<User> receivers = deliveries.stream()
                .collect(Collectors.toMap(
                        d -> d.getReceiver().getId(),
                        NotificationDelivery::getReceiver,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        // 알림 설정 조회 및 Map 변환
        Map<Long, NotificationSetting> settingMap = notificationSettingRepository
                .findAllByUserIn(receivers)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getUser().getId(),
                        s -> s,
                        (existing, replacement) -> existing
                ));

        // 발송 대상 유저 필터링
        List<User> eligibleReceivers = receivers.stream()
                .filter(user -> {
                    if (isMandatory) return true;

                    NotificationSetting setting = settingMap.get(user.getId());
                    if (setting == null) return true;

                    return isCategoryEnabled(setting, category);
                })
                .toList();

        if (eligibleReceivers.isEmpty()) {
            log.debug("[sendPushForDeliveries] 알림 설정을 충족하는 FCM 발송 대상이 없습니다. deliveryCount={}, category={}",
                    deliveries.size(), category);
            return;
        }

        // 활성 토큰을 IN절로 한 번에 조회
        List<String> allTokens = fcmTokenRepository.findAllActiveTokensByUsers(eligibleReceivers);

        if (allTokens.isEmpty()) {
            log.debug("[sendPushForDeliveries] 활성 FCM token이 없어 발송을 건너뜁니다. receiverCount={}",
                    eligibleReceivers.size());
            return;
        }

        // FCM 500개 제한에 맞춰 배치 분할 후 벌크 전송
        List<List<String>> batches = partitionTokens(allTokens, FCM_MULTICAST_LIMIT);
        int failedBatchCount = 0;
        FirebaseMessagingException firstFailure = null;
        for (List<String> batch : batches) {
            FirebaseMessagingException failure = sendMulticast(batch, event);
            if (failure != null) {
                failedBatchCount++;
                if (firstFailure == null) firstFailure = failure;
            }
        }
        if (failedBatchCount > 0) {
            log.error("[sendPushForDeliveries] FCM bulk 발송 batch 총 {}건 중 {}건을 실패했습니다. typeName={}, category={}, targetType={}, targetId={}",
                    batches.size(), failedBatchCount, event.getType().getTypeName(), event.getCategory(),
                    event.getTargetType(), event.getTargetId(), firstFailure);
        }
    }

    @Override
    public void sendPushForDelivery(NotificationDelivery delivery, NotificationEvent event) {
        // 단일 발송 시 delivery가 null인 경우 조기 리턴
        if (delivery == null) {
            log.warn("[sendPushForDelivery] FCM 발송 대상 NotificationDelivery가 null입니다.");
            return;
        }

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

    private FirebaseMessagingException sendMulticast(List<String> tokens, NotificationEvent event) {
        // iOS APNs 설정 추가
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setSound("default")
                        .setContentAvailable(true)
                        .build())
                .putHeader("apns-priority", "10")
                .build();

        // Android 우선순위 명시
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(
                        Notification.builder()
                                .setTitle(event.getTitle())
                                .setBody(event.getMessage())
                                .setImage(s3UrlUtil.toFullUrl(event.getImageKey()))
                                .build()
                )
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .putData("targetType", event.getTargetType() != null ? event.getTargetType().name() : "")
                .putData("targetId", event.getTargetId() != null ? String.valueOf(event.getTargetId()) : "")
                .putData("contextType", event.getContextType() != null ? event.getContextType().name() : "")
                .putData("contextId", event.getContextId() != null ? String.valueOf(event.getContextId()) : "")
                .putData("category", event.getCategory() != null ? event.getCategory().name() : "")
                .putData("typeName", event.getType() != null ? event.getType().getTypeName().name() : "")
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            fcmTokenDeactivationService.handleFailedTokens(tokens, response);
            log.info("[sendMulticast] FCM bulk 발송을 완료했습니다. tokenCount={}, successCount={}, failedCount={}",
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());
            return null;

        } catch (FirebaseMessagingException e) {
            log.warn("[sendMulticast] FCM bulk 발송 batch 처리에 실패했습니다. tokenCount={}, exception={}",
                    tokens.size(), e.getClass().getSimpleName());
            return e;
        }
    }
}
