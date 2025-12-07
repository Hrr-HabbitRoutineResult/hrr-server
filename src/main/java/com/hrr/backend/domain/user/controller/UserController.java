package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.dto.UserVerificationResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.service.UserService;
import com.hrr.backend.domain.user.service.UserVerificationService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description ="사용자 관련 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;
    private final UserVerificationService userVerificationService;


    // 닉네임 유효성 검사 API
    @Operation(
            summary = "닉네임 유효성 검사",
            description = "입력한 닉네임이 사용 가능한지 검사합니다."
    )
    @GetMapping("/nickname/check")
    public ApiResponse<Boolean> checkNickname(
            @RequestParam("nickname") String nickname
    ) {
        boolean available = userService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(SuccessCode.OK, available);
    }


    // 닉네임 설정 API
    @Operation(
            summary = "닉네임 설정",
            description = "회원가입 단계에서 닉네임을 설정합니다. 최대 10자, 중복 불가."
    )
    @PostMapping("/nickname")
    public ApiResponse<UserNicknameResponseDto> setNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserNicknameRequestDto request
    ) {
        User user = userDetails.getUser();
        UserNicknameResponseDto response = userService.setNickname(user, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

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

    // 내가 참가중인 챌린지 조회
    @GetMapping("/me/challenge/ongoing")
    @Operation(
            summary = "내가 참가중인 챌린지 목록 조회",
            description = "현재 로그인한 사용자가 참가중인 챌린지 목록을 조회합니다. " +
                    "ONGOING 상태의 챌린지만 반환됩니다."
    )
    public ApiResponse<SliceResponseDto<UserResponseDto.OngoingChallengeDto>> getMyOngoingChallenges(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(0)
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,

            @RequestParam(name = "size", defaultValue = "10")
            @Min(1) @Max(100)
            @Parameter(description = "페이지당 데이터 개수", example = "10") int size
    ) {
        Long userId = customUserDetails.getUser().getId();
        SliceResponseDto<UserResponseDto.OngoingChallengeDto> response =
                userService.getOngoingChallenges(userId, page, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }


    // 다른 사용자가 참가중인 챌린지 조회
    @GetMapping("/{userId}/challenge/ongoing")
    @Operation(
            summary = "다른 사용자가 참가중인 챌린지 목록 조회",
            description = "특정 사용자가 참가중인 챌린지 목록을 조회합니다. " +
                    "ONGOING 상태의 챌린지만 반환됩니다.\n"
    )
    public ApiResponse<SliceResponseDto<UserResponseDto.OngoingChallengeDto>> getUserOngoingChallenges(
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

	@GetMapping("/search")
	@Operation(summary = "챌린저 검색", description = "검색 키워드가 닉네임에 포함된 사용자를 조회합니다.")
	public ApiResponse<SliceResponseDto<UserResponseDto.ProfileDto>> searchChallengers(
		@RequestParam(name = "keyword")
		@NotBlank(message = "검색어는 필수입니다.") String keyword,

		// 페이징
		@Min(0)
		@RequestParam(name = "page", defaultValue = "0") int page, // 페이지 번호 (0부터 시작)
		@Min(1)
		@RequestParam(name = "size", defaultValue = "10") int size, // 페이지 크기)

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails
		)
	{
		SliceResponseDto<UserResponseDto.ProfileDto> response = userService.searchChallengers(customUserDetails.getUser(), keyword, page, size);

		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

    @GetMapping("/challenges/history")
    @Operation(
            summary = "내 챌린지 인증 기록 조회",
            description = "현재 로그인한 사용자가 참여한 모든 챌린지의 인증 기록을 최신순으로 조회합니다."
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
                                            "verificationId": 1,
                                            "challengeId": 101,
                                            "challengeTitle": "미라클 모닝",
                                            "title": "해피뉴이어! 올해 마지막 인증 올립니다",
                                            "content": "여기엔 상세내용이 들어가유~",
                                            "imageUrl": "https://example.com/verification_image_1.jpg",
                                            "verifiedAt": "2025-09-18T08:00:00Z"
                                          },
                                          {
                                            "verificationId": 2,
                                            "challengeId": 102,
                                            "challengeTitle": "매일 책 10페이지 읽기",
                                            "title": "오늘의 독서 인증",
                                            "content": "몰입의 즐거움 완독!",
                                            "imageUrl": "https://example.com/verification_image_2.jpg",
                                            "verifiedAt": "2025-09-13T22:30:00Z"
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
    public ApiResponse<SliceResponseDto<UserVerificationResponseDto.VerificationItemDto>> getVerificationHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @RequestParam(name = "page", defaultValue = "0")
            @Min(0)
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") int page,

            @RequestParam(name = "size", defaultValue = "10")
            @Min(1) @Max(100)
            @Parameter(description = "페이지당 데이터 개수", example = "10") int size
    ) {
        Long userId = customUserDetails.getUser().getId();
        SliceResponseDto<UserVerificationResponseDto.VerificationItemDto> response =
                userVerificationService.getVerificationHistory(userId, page, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
