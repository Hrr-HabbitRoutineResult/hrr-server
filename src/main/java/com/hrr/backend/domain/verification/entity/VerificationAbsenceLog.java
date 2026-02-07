package com.hrr.backend.domain.verification.entity;

import java.time.LocalDate;


import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
	name = "verification_absence_log",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_round_record_date",
			columnNames = {"round_record_id", "absence_date"}
		)
	}
)
// 미인증 기록을 저장하기 위한 엔티티
public class VerificationAbsenceLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "round_record_id", nullable = false)
	private RoundRecord roundRecord;

	@Column(name = "absence_date", nullable = false)
	private LocalDate absenceDate; // 미인증이 발생한 날짜
}
