package com.hrr.backend.domain.user.entity;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.enums.Category;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "random_mission")
public class RandomMission extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 15)
	private String title;

	@Column(name = "content", nullable = false, length = 30)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private Category category;

	@Column(name = "image_key")
	private String imageKey;
}
