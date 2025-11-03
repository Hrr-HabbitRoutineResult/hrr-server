package com.hrr.backend.global.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponseDto<T> {

	private final List<T> content;

	private final int currentPage;    // 현재 페이지 번호 (1부터 시작)
	private final int totalPages;     // 전체 페이지 수
	private final long totalElements; // 전체 데이터 개수
	private final int size;           // 한 페이지당 데이터 개수 (요청한 size)
	private final boolean isFirst;    // 첫 번째 페이지 여부
	private final boolean isLast;     // 마지막 페이지 여부

	public PageResponseDto(Page<T> page) {
		this.content = page.getContent(); // 실제 데이터 리스트

		// 화면 표기를 위해 1-based index로 변환
		this.currentPage = page.getNumber() + 1;

		this.totalPages = page.getTotalPages();
		this.totalElements = page.getTotalElements();
		this.size = page.getSize();
		this.isFirst = page.isFirst();
		this.isLast = page.isLast();
	}
}
