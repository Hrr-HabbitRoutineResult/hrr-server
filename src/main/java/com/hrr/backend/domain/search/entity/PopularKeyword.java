package com.hrr.backend.domain.search.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "popular_keyword")
public class PopularKeyword {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// upsert를 위한 unique 설정; 인덱싱 자동
	@Column(unique = true, nullable = false)
	private String keyword;

	@Column(name = "total_count", nullable = false)
	private Long totalCount = 0L;
}
