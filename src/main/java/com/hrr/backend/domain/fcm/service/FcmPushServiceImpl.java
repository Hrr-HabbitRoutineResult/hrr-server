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

    @Override
    public void sendPushForDeliveries(List<NotificationDelivery> deliveries, NotificationEvent event) {
        if (deliveries == null || deliveries.isEmpty()) return;

        // 필수 필드(Event, Type)에 대한 조기 리턴
        if (event == null || event.getType() == null) {
            log.warn("FCM 발송 스킵: NotificationEvent 또는 NotificationType이 null입니다.");
            return;
        }

        boolean isMandatory = event.getType().isMandatory();
        NotificationCategory category = event.getCategory();

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
                    // 필수 알림인 경우 무조건 발송
                    if (isMandatory) return true;

                    // 필수 알림이 아닌데 카테고리가 없으면 필터링 조건 확인 불가로 발송 제외
                    if (category == null) {
                        log.warn("FCM 발송 필터링: 필수 알림이 아니지만 카테고리가 null입니다. (UserId={})", user.getId());
                        return false;
                    }

                    NotificationSetting setting = settingMap.get(user.getId());
                    if (setting == null) return true; // 설정 레코드가 없으면 기본 허용

                    return isCategoryEnabled(setting, category);
                })
                .toList();

        if (eligibleReceivers.isEmpty()) {
            log.debug("발송 대상 없음: category={}, mandatory={}", category, isMandatory);
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
        // 단일 발송 시 delivery가 null인 경우 조기 리턴
        if (delivery == null) {
            log.warn("FCM 발송 스킵: NotificationDelivery가 null입니다.");
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

    private void sendMulticast(List<String> tokens, NotificationEvent event, int totalReceivers) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(
                        Notification.builder()
                                .setTitle(event.getTitle())
                                .setBody(event.getMessage())
                                .setImage(s3UrlUtil.toFullUrl(event.getImageKey()))
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
                String token = tokens.get(i); // 현재 처리 중인 토큰

                if (errorCode == MessagingErrorCode.UNREGISTERED) {
                    fcmTokenRepository.deactivateAllByToken(token);
                    // 마스킹 메서드 적용
                    log.info("유효하지 않은 FCM 토큰 전체 비활성화 완료: token={}", maskToken(token));
                }
                else if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    // 원문 대신 마스킹된 토큰 로그 출력
                    log.warn("FCM 메시지 페이로드 또는 토큰 형식이 잘못되었습니다. (INVALID_ARGUMENT): token={}, message={}",
                            maskToken(token), sendResponse.getException().getMessage());
                }
            }
        }
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "<empty>";
        }
        // 앞 20자만 보여주고 나머지는 마스킹 처리
        int visible = Math.min(20, token.length());
        return token.substring(0, visible) + "...";
    }
}