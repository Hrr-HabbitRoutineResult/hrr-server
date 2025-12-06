package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ID로 유저 조회 (soft delete 미적용)
    Optional<User> findById(Long id);
    // Kakao ID
    Optional<User> findByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);

	/**
	 * 닉네임에 키워드가 포함된 사용자 목록을 Slice 형태로 조회
	 * * @param keyword 검색할 닉네임 키워드
	 * @param pageable 페이징 요청 정보 (page, size)
	 * @return Slice<User> - 데이터 목록과 다음 페이지 존재 여부(hasNext)를 포함
	 */
	Slice<User> findByNicknameContaining(String keyword, Pageable pageable);
}
