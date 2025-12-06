package com.hrr.backend.domain.user.service;

import java.util.List;
import java.util.stream.Collectors;

import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 (조회 성능 최적화)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserChallengeRepository userChallengeRepository;

    private final ChallengeRepository challengeRepository;
    private final VerificationRepository verificationRepository;

    private final S3UrlUtil s3UrlUtil;

    // 프로필 조회 관련

    @Override
    public UserResponseDto.ProfileDto getUserProfile(Long userId, Long currentUserId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 팔로잉 여부 확인
        Boolean isFollowing = checkIfFollowing(currentUserId, userId);

        return UserResponseDto.ProfileDto.from(user, isFollowing);
    }

    @Override
    public SliceResponseDto<UserResponseDto.OngoingChallengeDto> getOngoingChallenges(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성 (0-based index)
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 참가중인 챌린지 조회
        Slice<UserResponseDto.OngoingChallengeDto> slice =
                userChallengeRepository.findOngoingChallengesByUser(user, pageable);

        // URL 변환 로직 교체
        slice.getContent().forEach(dto ->
                dto.setThumbnailUrl(s3UrlUtil.toFullUrl(dto.getThumbnailUrl()))
        );

		// 인증 완료 여부 추가 - 오늘 포함 가장 최근 인증 요일에 완료 여부
		slice.getContent().forEach(dto -> {
			Long challengeId = dto.getChallengeId();
			// 챌린지 요일/인증 시간 로딩
			Challenge challenge = challengeRepository.findByIdWithDays(challengeId)
					.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

			// 유저-챌린지 매핑 (userChallengeId 필요)
			Long userChallengeId = userChallengeRepository
					.findByUserIdAndChallengeId(userId, challengeId)
					.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND))
					.getId();

			// 오늘 포함 가장 최근 인증 요일 계산
			LocalDate targetDate = findLastestDateIncludingToday(challenge);
			boolean verified = false;
			if (targetDate != null) {
				LocalDateTime start = LocalDateTime.of(targetDate, challenge.getVerifyStartTime());
				LocalDateTime end = LocalDateTime.of(targetDate, challenge.getVerifyEndTime());
				verified = verificationRepository.existsTodayVerification(	// 오늘 완료 여부를 확인하기 위한 메소드지만 파라미터 조정으로 활용
						userChallengeId,
						VerificationStatus.COMPLETED,
						start,
						end
				);
			}

			// DTO 반영
			dto.setVerified(verified);
		});


        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(slice);
    }

    private Boolean checkIfFollowing(Long currentUserId, Long targetUserId) {
        // 비로그인 상태
        if (currentUserId == null) {
            return false;
        }

        // 본인 프로필 조회
        if (currentUserId.equals(targetUserId)) {
            return false;
        }

        // 다른 사람 프로필 조회
        return followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }

    // 닉네임 유효성 검사 (중복 체크)
    @Override
    public boolean isNicknameAvailable(String rawNickname) {
        String nickname = normalize(rawNickname);
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    public UserResponseDto.MyInfoDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        return UserResponseDto.MyInfoDto.from(user);
    }

    // 닉네임 설정
    @Override
    @Transactional
    public UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request) {

        // 온보딩 상태 검증
        if (user.getLoginStatus() != LoginStatus.TERMS_DONE) {
            throw new GlobalException(ErrorCode.INVALID_LOGIN_STATUS_FOR_NICKNAME);
        }

        String nickname = normalize(request.getNickname());

        // 닉네임 중복 검사만 수행 (빈값/길이는 DTO Validation에서 수행)
        if (!isNicknameAvailable(nickname)) {
            throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 닉네임 저장 + ACTIVE 전환
        user.updateNickname(nickname);
        user.updateLoginStatus(LoginStatus.ACTIVE);
        userRepository.save(user);

        return UserNicknameResponseDto.builder()
                .nickname(nickname)
                .message("사용 가능한 닉네임이에요.")
                .nextStep(user.determineNextStep())
                .build();
    }

	/**
	 * 키워드가 닉네임에 포함된 사용자 조회
	 *
	 * @param keyword 검색어
	 * @param page 페이지 번호
	 * @param size 조회할 데이터 개수
	 */
	@Override
	public SliceResponseDto<UserResponseDto.ProfileDto> searchChallengers(User user, String keyword, int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		// DB 조회
		Slice<User> usersSlice = userRepository.findByNicknameContaining(keyword, pageable);

		// DTO 변환 및 팔로잉 여부 추가
		List<UserResponseDto.ProfileDto> profileDtos = usersSlice.getContent().stream()
			.filter(target -> !target.getId().equals(user.getId()))
			.map(target -> {
				// 현재 사용자(user)가 검색된 사용자(target)를 팔로우하고 있는지 확인
				boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(user.getId(), target.getId());

				// DTO 생성 및 매핑
				return UserResponseDto.ProfileDto.builder()
					.userId(target.getId())
					.profileImage(target.getProfileImage())
					.nickname(target.getNickname())
					.level(target.getUserLevel())
					.isFollowing(isFollowing) // 팔로우 여부 추가
					.build();
			})
			.collect(Collectors.toList());

		// SliceResponseDto 생성 및 반환
		// SliceImpl을 사용하여 DTO List와 기존 Slice의 메타데이터(Pageable, hasNext)를 결합
		Slice<UserResponseDto.ProfileDto> resultSlice = new SliceImpl<>(
			profileDtos,
			pageable,
			usersSlice.hasNext() // 다음 페이지 존재 여부
		);

		// 최종 응답 DTO 반환
		return SliceResponseDto.of(resultSlice);
	}

	private String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim();
    }

    // 오늘 포함, 챌린지의 인증 요일 중 가장 최근 날짜 계산 (최대 7일 탐색)
    private LocalDate findLastestDateIncludingToday(Challenge challenge) {
        // 챌린지의 요일 집합
        var targetDays = challenge.getChallengeDays().stream()
                .map(ChallengeDayJoin::getDayOfWeek)
                .collect(java.util.stream.Collectors.toSet());

        if (targetDays.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = today.minusDays(i);
            ChallengeDays asEnum = ChallengeDays.from(candidate.getDayOfWeek());
            if (targetDays.contains(asEnum)) {
                return candidate;
            }
        }

        return null;
    }
}
