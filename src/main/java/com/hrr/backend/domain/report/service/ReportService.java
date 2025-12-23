package com.hrr.backend.domain.report.service;

import com.hrr.backend.domain.report.dto.ReportRequestDto;

public interface ReportService {

	// 인증 게시글 신고
	void reportVerificationPost(Long reporterId, ReportRequestDto request);

	// 사용자 신고
	void reportUser(Long reporterId, ReportRequestDto request);
}
