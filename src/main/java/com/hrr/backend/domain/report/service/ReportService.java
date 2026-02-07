package com.hrr.backend.domain.report.service;

import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.user.entity.User;

public interface ReportService {

	// 부실 인증 신고
	void reportWeakVerification(User reporter, Long verificationId);

	// 인증 게시글 신고
	void reportVerificationPost(User reporter, ReportRequestDto request);

	// 사용자 신고
	void reportUser(User reporter, ReportRequestDto request);
}
