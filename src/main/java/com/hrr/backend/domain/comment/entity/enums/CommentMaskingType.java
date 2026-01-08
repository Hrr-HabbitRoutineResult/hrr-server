package com.hrr.backend.domain.comment.entity.enums;

public enum CommentMaskingType {
    NONE,       // 마스킹 없음
    BLOCKED,    // 내가 차단한 사용자
    DELETED,    // 삭제된 댓글
    INACTIVE    // 탈퇴한 사용자
}
