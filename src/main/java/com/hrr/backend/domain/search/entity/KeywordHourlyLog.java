package com.hrr.backend.domain.search.entity;

import java.time.LocalDateTime;

import org.checkerframework.checker.units.qual.N;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "keyword_hourly_log",
	indexes = {
		// 조회 및 집계 성능 향상을 위한 인덱싱
		@Index(name = "idx_keyword_hourly_log_keyword", columnList = "keyword"),
		@Index(name = "idx_keyword_hourly_log_hour", columnList = "hour")
	}
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * 검색 데이터를 보존하기 위한 로그 테이블
 */
public class KeywordHourlyLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String keyword;

	@Column(nullable = false)
	private Long count;

	@Column(nullable = false)
	private LocalDateTime hour;
}
