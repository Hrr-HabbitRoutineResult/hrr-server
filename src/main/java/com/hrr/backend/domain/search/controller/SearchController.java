package com.hrr.backend.domain.search.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
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

		return ApiResponse.onSuccess(SuccessCode.OK, null);	// result가 필요없어서 null로 설정
	}

	@GetMapping("/popular-keyword")
	@Operation(summary = "인기 검색어 조회", description = "현재 인기 검색어 Top N을 반환합니다.")
	public ApiResponse<List<String>> getPopularKeywords(
		// 기본값 10개로 설정
		@RequestParam(name = "limit", defaultValue = "10") int limit)
	{
		List<String> keywords = searchService.getTopNPopularKeywords(limit);

		// List<String> 데이터를 ApiResponse의 data 필드에 담아 반환
		return ApiResponse.onSuccess(SuccessCode.OK, keywords);
	}

}
