package com.hrr.backend.domain.user.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.global.common.enums.Category;

public interface RandomMissionRepository extends JpaRepository<RandomMission, Long> {

	// 카테고리 리스트에 해당하는 미션들을 조회
	List<RandomMission> findByCategoryIn(List<Category> categoryList);
}
