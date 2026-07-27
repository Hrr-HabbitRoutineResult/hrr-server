package com.hrr.backend.domain.point.event;

/**
 * 인증글 생성 완료(커밋 후) 시점에 첫 인증/주차 퍼펙트 포인트 지급을 트리거하기 위한 이벤트.
*/
public record VerificationPointTriggerEvent(Long verificationId) {
}