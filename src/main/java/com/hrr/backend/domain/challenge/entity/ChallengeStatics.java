package com.hrr.backend.domain.challenge.entity;

import com.hrr.backend.global.common.enums.FavorType;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "challenge_statics")
@Getter
@RequiredArgsConstructor
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeStatics {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "challenge_id")
	private Challenge challenge;

	@Enumerated(EnumType.STRING)
	@Column(name = "favor_type", nullable = false)
	private FavorType favorType;	// 어떤 종류의 선호인지(성별, 연령대 등)

	@Column(name = "favor_value", nullable = false)
	private String favorValue;	// 선호 타입에 대한 값(여성, 20대 등)

	@Column(name = "stat_number")
	private Integer number; // 챌린지 내 특정 선호값에 해당하는 유저 수

	/**
	 * 통계 수치를 새로운 값으로 업데이트합니다.
	 */
	public void updateNumber(Integer newCount) {

        if (newCount != null && newCount < 0) {
            throw new GlobalException(ErrorCode.STATICS_NEGATIVE_VALUE);
        	}
        this.number = (newCount == null) ? 0 : newCount;
	}

}
