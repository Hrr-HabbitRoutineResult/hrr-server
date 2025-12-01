package com.hrr.backend.domain.term.controller;

import com.hrr.backend.domain.term.dto.TermResponseDto;
import com.hrr.backend.domain.term.service.TermService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Terms", description = "약관 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermController {

    private final TermService termService;

    @Operation(summary = "약관 목록 조회", description = "약관의 제목 및 필수 여부를 조회합니다.")
    @GetMapping
    public ApiResponse<List<TermResponseDto.TermSummary>> getTermList() {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                termService.getTermList()
        );
    }

    @Operation(summary = "약관 상세 조회", description = "약관의 상세 내용을 조회합니다.")
    @GetMapping("/{termId}")
    public ApiResponse<TermResponseDto.TermDetail> getTermDetail(
            @PathVariable Long termId
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                termService.getTermDetail(termId)
        );
    }
}
