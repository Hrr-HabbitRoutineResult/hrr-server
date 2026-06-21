package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.converter.NotificationConverter;
import com.hrr.backend.domain.notification.dto.NotificationRequestDto;
import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.repository.NotificationRepository;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationConverter notificationConverter;
    private final RoundRecordRepository roundRecordRepository;

    private final S3UrlUtil s3UrlUtil;

    @Override
    public SliceResponseDto<NotificationResponseDto.InfoDto> getNotificationList(
            User user, NotificationCategory category, int page, int size) {

        // 0-based page index 사용
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 Slice 조회
        Slice<NotificationDelivery> deliverySlice = notificationRepository
                .findMyNotifications(user, category, pageable);

        // Slice 내부의 엔티티를 DTO로 변환
        List<NotificationResponseDto.InfoDto> dtos = deliverySlice.getContent().stream()
                .map(delivery -> {
                    String fullUrl = s3UrlUtil.toFullUrl(delivery.getEvent().getImageKey());
                    return notificationConverter.toInfoDto(delivery, fullUrl);
                })
                .toList();

        List<Long> challengeExtensionRoundIds = dtos.stream()
                .filter(dto -> dto.getType() == NotificationTypeName.CHALLENGE_EXTENSION)
                .map(NotificationResponseDto.InfoDto::getContextId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, RoundRecord> roundRecordByRoundId = challengeExtensionRoundIds.isEmpty()
                ? Collections.emptyMap()
                : roundRecordRepository.findByUserAndRoundIds(user, challengeExtensionRoundIds).stream()
                .collect(Collectors.toMap(
                        record -> record.getRound().getId(),
                        Function.identity(),
                        (first, ignored) -> first
                ));

        // 연장 안내 알림일 경우 응답 여부 계산
        dtos.forEach(dto -> {
            if (dto.getType() == NotificationTypeName.CHALLENGE_EXTENSION) {
                RoundRecord record = roundRecordByRoundId.get(dto.getContextId());
                boolean responded = record != null && record.getNextRoundIntent() != NextRoundIntent.UNDECIDED;
                dto.setIsResponded(responded);
            } else {
                dto.setIsResponded(null);
            }
        });

        return new SliceResponseDto<>(new SliceImpl<>(dtos, pageable, deliverySlice.hasNext()));
    }

    @Override
    @Transactional
    public NotificationResponseDto.ReadResultDto markAsRead(User user, Long notificationId) {
        NotificationDelivery delivery = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!delivery.getReceiver().getId().equals(user.getId())) {
            throw new GlobalException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        delivery.markAsRead();

        return NotificationResponseDto.ReadResultDto.from(delivery);
    }

    @Override
    public NotificationResponseDto.SettingInfoDto getNotificationSettings(User user) {
        NotificationSetting setting = notificationSettingRepository.findByUser(user)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));

        return notificationConverter.toSettingInfoDto(setting);
    }

    @Override
    @Transactional
    public NotificationResponseDto.SettingInfoDto updateNotificationSettings(User user, NotificationRequestDto.UpdateSettingDto dto) {
        // 유저의 설정 정보 조회
        NotificationSetting setting = notificationSettingRepository.findByUser(user)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));

        // '전체 일시 중단' 스위치를 건드렸을 경우
        if (dto.getIsAllPaused() != null) {
            if (dto.getIsAllPaused()) {
                setting.pauseAll(); // 모든 필드 false로 변경
            } else {
                setting.resumeAll(); // 모든 필드 true로 변경
            }
        }
        // 개별 스위치를 건드렸을 경우 (null이 아닌 필드만 업데이트)
        else {
            setting.update(
                    dto.getIsChallengeEnabled(),
                    dto.getIsVerificationEnabled(),
                    dto.getIsFollowEnabled(),
                    dto.getIsBadgeEnabled()
            );
        }

        // 변경된 결과 반환
        return notificationConverter.toSettingInfoDto(setting);
    }

    @Override
    public NotificationResponseDto.UnreadStatusDto checkUnreadStatus(User user) {
        boolean hasUnread = notificationRepository.existsByReceiverAndIsReadFalse(user);

        return NotificationResponseDto.UnreadStatusDto.builder()
                .hasUnread(hasUnread)
                .build();
    }
}
