package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.service.UserService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description ="사용자 관련 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "타인 사용자 기본 정보 조회", description = "특정 사용자의 프로필 정보를 조회합니다.\n")
    public ApiResponse<UserResponseDto.ProfileDto> getUserProfile(
            @PathVariable
            @Parameter(description = "조회할 사용자 ID", example = "999") Long userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 인증된 사용자 ID 추출 (비로그인 시 null)
        Long currentUserId = (customUserDetails != null) ? customUserDetails.getUser().getId() : null;

        UserResponseDto.ProfileDto profile = userService.getUserProfile(userId, currentUserId);
        return ApiResponse.onSuccess(SuccessCode.OK, profile);
    }

    @GetMapping("/{userId}/challenge/ongoing")
    @Operation(
            summary = "참가중인 챌린지 목록 조회",
            description = "현재 로그인한 사용자가 참가중인 챌린지 목록을 조회합니다. " +
                    "ONGOING 상태의 챌린지만 반환됩니다.\n"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                    {
                                      "resultType": "SUCCESS",
                                      "error": null,
                                      "success": {
                                        "content": [
                                          {
                                            "challengeId": 301,
                                            "title": "자잘자잘",
                                            "description": "하루 5분씩 무엇이든 꼭 해야...",
                                            "image": "http://example.com/challenge_301.jpg",
                                            "currentRound": 6
                                          },
                                          {
                                            "challengeId": 302,
                                            "title": "공부합시다",
                                            "description": "매일 아침 영어 공부 1시간",
                                            "image": "http://example.com/challenge_302.jpg",
                                            "currentRound": 15
                                          }
                                        ],
                                        "currentPage": 0,
                                        "size": 10,
                                        "first": true,
                                        "last": false,
                                        "hasNext": true
                                      }
                                    }
                                    """
                            )
                    )
            )
    })
    public ApiResponse<SliceResponseDto<UserResponseDto.OngoingChallengeDto>> getOngoingChallenges(
            @PathVariable
            @Parameter(description = "조회할 사용자 ID", example = "999") Long userId,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(0)
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,

            @RequestParam(name = "size", defaultValue = "10")
            @Min(1) @Max(100)
            @Parameter(description = "페이지당 데이터 개수", example = "10") int size
    ) {
        SliceResponseDto<UserResponseDto.OngoingChallengeDto> response =
                userService.getOngoingChallenges(userId, page, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 정보를 조회합니다.")
    public ApiResponse<UserResponseDto.MyInfoDto> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        UserResponseDto.MyInfoDto myInfo = userService.getMyInfo(customUserDetails.getUser().getId());
        return ApiResponse.onSuccess(SuccessCode.OK, myInfo);
    }
}