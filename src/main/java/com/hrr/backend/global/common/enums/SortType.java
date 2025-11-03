package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@RequiredArgsConstructor
public enum SortType {
	LATEST("createdAt", Sort.Direction.DESC), // 최신순: 생성일 내림차순
	OLDEST("createdAt", Sort.Direction.ASC),  // 오래된순: 생성일 오름차순
	POPULAR("likeCount", Sort.Direction.DESC); // 인기순: 좋아요 수 내림차순 (예시)

	private final String property;    // DB 정렬 기준이 되는 필드명
	private final Sort.Direction direction; // 정렬 방향

	//Sort 객체를 생성하여 반환; JPA Repository 메소드의 인자로 전달 시 자동으로 정렬
	public Sort toSort() {
		return Sort.by(this.direction, this.property);
	}
}
