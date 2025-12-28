package com.hrr.backend.domain.report.dto;

import com.hrr.backend.global.common.enums.ReportReason;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

/**
 * 기본적인 신고 틀을 동일하며, 추후 구현될 부실 인증 신고는 인증 사유만 null 값으로 넣는 등의 처리가 가능하기에
 * 내부 클래스를 따로 두지 않고 공통 DTO로 진행
 */
@Getter
public class ReportRequestDto {
	// 신고 대상, 선택 사유, 직접 입력 사유

	@Schema(description = "신고 대상 ID", example = "1")
	@NotNull(message = "신고 대상 ID는 필수입니다.")
	private Long targetId;

	@Schema(
		description = "신고 선택 사유",
		example = "ABUSIVE_LANGUAGE",
		allowableValues = {
			"ABUSIVE_LANGUAGE", "SEXUAL_OR_OBSCENE", "SPAM_OR_SCAM",
			"PERSONAL_INFO_REQUEST", "ILLEGAL_CONTENT_SHARE", "OTHER"
		}
	)
	@NotNull(message = "신고 사유를 선택해 주세요.")
	private ReportReason reason;

	@Schema(description = "직접 입력 사유 (기타)", example = "부적절한 광고 이미지가 포함되어 있습니다.")
	private String description;
}
