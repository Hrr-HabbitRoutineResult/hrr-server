package com.hrr.backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
	name = "user_mission", // 테이블명: UserMission
	uniqueConstraints = {
		// 사용자는 하루에 하나의 미션만 가질 수 있도록 유니크 제약 조건 설정
		@UniqueConstraint(
			name = "UK_user_mission_date",
			columnNames = {"user_id", "date"}
		)
	}
)
public class UserMission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mission_id")
	private RandomMission mission;

	@Column(name = "date", nullable = false)
	private LocalDate date;

	@Setter
	@Column(name = "is_completed", nullable = false)
	private Boolean isCompleted;

	@Setter
	@Column(name = "image_key")
	private String imageKey;
}
