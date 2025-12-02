package com.hrr.backend.domain.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.search.entity.KeywordHourlyLog;

public interface KeywordHourlyLogRepository extends JpaRepository<KeywordHourlyLog, Long> {
}
