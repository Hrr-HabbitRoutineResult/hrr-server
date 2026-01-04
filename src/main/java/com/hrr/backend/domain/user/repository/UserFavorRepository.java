package com.hrr.backend.domain.user.repository;

import java.util.List;
import java.util.Optional;

import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.global.common.enums.Category;

public interface UserFavorRepository extends JpaRepository<UserFavor, Long> {

	// 사용자의 선호 카테고리 조회
	@Query("SELECT uf.category FROM UserFavor uf WHERE uf.user.id = :userId")
	List<Category> findCategoriesByUserId(Long userId);

    Optional<UserFavor> findByUserId(Long userId);
}
