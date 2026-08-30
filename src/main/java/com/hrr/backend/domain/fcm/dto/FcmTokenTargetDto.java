package com.hrr.backend.domain.fcm.dto;

/** FCM에는 token만 전달하고, 내부 ID는 실패한 대상 추적에만 사용한다. */
public record FcmTokenTargetDto(
		Long fcmTokenId,
		Long userId,
		String token
) {
	@Override
	public String toString() {
		return "FcmTokenTargetDto[fcmTokenId=" + fcmTokenId
				+ ", userId=" + userId
				+ ", token=[REDACTED]]";
	}
}
