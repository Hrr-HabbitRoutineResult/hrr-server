package com.hrr.backend.domain.search.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.search.service.SearchService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Tag(name = "Search", description = "검색 관련 API")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

	private final SearchService searchService;

	@PostMapping("/count")
	@Operation(summary = "인기 검색어 카운트 증가", description = "검색어의 인기 검색 카운트를 1 증가시킵니다. 응답의 result는 null을 반환합니다. ")
	public ApiResponse<?> incrementSearchCount(
		@RequestParam(name = "keyword")
		@NotBlank(message = "검색어는 필수입니다.") String keyword)
	{
		// 검색 카운트 증가 서비스 호출
		searchService.incrementSearchCount(keyword);

		return ApiResponse.onSuccess(SuccessCode.OK, null);	// result가
	}
}
