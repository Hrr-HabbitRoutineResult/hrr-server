package com.hrr.backend.global.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponseDto<T> {

	private final List<T> content;

	private final int currentPage;    // 현재 페이지 번호 (1부터 시작)
	private final int size;           // 한 페이지당 데이터 개수 (요청한 size)
	private final boolean isFirst;    // 첫번째 페이지 여부
	private final boolean isLast;     // 마지막 페이지 여부
	private final boolean hasNext;	  // 다음 페이지 존재 여부

	public SliceResponseDto(Slice<T> slice) {
		this.content = slice.getContent(); // 실제 데이터 리스트
		this.currentPage = slice.getNumber() + 1;	// 화면 표기를 위해 1-based index로 변환
		this.size = slice.getSize();
		this.isFirst = slice.isFirst();
		this.isLast = slice.isLast();
		this.hasNext = slice.hasNext();
	}
}
