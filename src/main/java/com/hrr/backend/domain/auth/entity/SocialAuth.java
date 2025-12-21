package com.hrr.backend.domain.auth.entity;

import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "social_auth", indexes = {
	@Index(name = "idx_social_id_type", columnList = "social_id, social_type")	// 문자열 검색의 최적화를 위해 인덱스 도입
})
public class SocialAuth extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "social_type", nullable = false)
	private SocialType socialType;

	@Column(name = "social_id", nullable = false)
	private String socialId; // 애플 sub, 네이버 id, 카카오 id(String 변환)

	/**
	 * 애플이나 네이버처럼 탈퇴 시 RT 가 필요한 경우를 위해 필드 추가
	 * TEXT 타입을 사용하여 긴 토큰 값도 저장 가능
	 */
	@Column(name = "social_refresh_token", columnDefinition = "TEXT")
	private String socialRefreshToken;
}
