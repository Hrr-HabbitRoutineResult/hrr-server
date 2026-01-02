package com.hrr.backend.domain.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.hrr.backend.domain.user.entity.User;

public interface UserRepositoryCustom {

	/**
	 * 닉네임에 키워드가 포함된 사용자 목록을 Slice 형태로 조회
	 * * @param keyword 검색할 닉네임 키워드
	 * @param pageable 페이징 요청 정보 (page, size)
	 * @return Slice<User> - 데이터 목록과 다음 페이지 존재 여부(hasNext)를 포함
	 */
	Slice<User> findByNicknameContaining(String keyword, User user, Pageable pageable);
}
