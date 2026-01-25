package com.hrr.backend.domain.user.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrr.backend.domain.user.dto.*;
import com.hrr.backend.domain.user.event.ProfileImageDeletedEvent;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.hrr.backend.domain.follow.repository.FollowRepository;
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
@Validated
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserBlockRepository userBlockRepository;

    private final ChallengeRepository challengeRepository;
    private final VerificationRepository verificationRepository;

    private final S3UrlUtil s3UrlUtil;
    private final ApplicationEventPublisher eventPublisher;


    // 다른 사용자 프로필 조회 관련
    @Override
    public UserResponseDto.ProfileDto getUserProfile(Long userId, User currentUser) {
        // 사용자 조회 (탈퇴자는 제외, 차단자는 허용)
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴 또는 비활성 상태 체크
        if (targetUser.isNotActive()) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        // 차단 여부 확인 (양방향)
        boolean isBlockedByMe = userBlockRepository.existsByBlockerAndBlocked(currentUser, targetUser);
        boolean isBlockedByOther = userBlockRepository.existsByBlockerAndBlocked(targetUser, currentUser);

        if (isBlockedByOther) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        boolean isFollowing = !isBlockedByMe && checkIfFollowing(currentUser.getId(), targetUser.getId());

        return UserResponseDto.ProfileDto.from(targetUser, isFollowing, isBlockedByMe);
    }

    @Override
    public SliceResponseDto<UserResponseDto.OngoingChallengeDto> getOngoingChallenges(
            Long userId,
            User currentUser,
            int page,
            int size
    ) {
        // 사용자 조회
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 타인 프로필 조회 시에만 체크
        if (!userId.equals(currentUser.getId())) {

            // 탈퇴/비활성 사용자 체크
            if (targetUser.isNotActive()) {
                throw new GlobalException(ErrorCode.USER_NOT_FOUND);
            }

            // 상대방이 나를 차단했는지 체크
            if (userBlockRepository.existsByBlockerAndBlocked(targetUser, currentUser)) {
                throw new GlobalException(ErrorCode.USER_NOT_FOUND);
            }

            // 내가 상대방을 차단했는지 체크
            if (userBlockRepository.existsByBlockerAndBlocked(currentUser, targetUser)) {
                return new SliceResponseDto<>(new SliceImpl<>(java.util.Collections.emptyList(), PageRequest.of(page, size), false));
            }
        }

        // Pageable 객체 생성 (0-based index; 이미 controller에서 -1 처리 완료)
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 참가중인 챌린지 조회
        Slice<UserResponseDto.OngoingChallengeDto> slice =
                userChallengeRepository.findOngoingChallengesByUser(targetUser, pageable);

        // URL 변환 로직 교체
        slice.getContent().forEach(dto ->
                dto.setThumbnailUrl(s3UrlUtil.toFullUrl(dto.getThumbnailUrl()))
        );

        // 인증 완료 여부 추가
        slice.getContent().forEach(dto -> {
            Long challengeId = dto.getChallengeId();

            // 챌린지 요일/인증 시간 로딩
            Challenge challenge = challengeRepository.findByIdWithDays(challengeId)
                    .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

            // java의 DayOfWeek를 ChallengeDays로 변환
            DayOfWeek todayDayOfWeek = LocalDate.now().getDayOfWeek();
            ChallengeDays todayChallengeDay = ChallengeDays.from(todayDayOfWeek);

            // 오늘이 인증하는 날인지 체크
            boolean isRegistrationDay = challenge.getChallengeDays().stream()
                    .anyMatch(day -> day.getDayOfWeek() == todayChallengeDay);

            boolean verified = false;

            if (isRegistrationDay) {
                // 유저-챌린지 매핑 (userChallengeId 필요)
                Long userChallengeId = userChallengeRepository
                        .findByUserIdAndChallengeId(targetUser.getId(), challengeId)
                        .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND))
                        .getId();

                // 오늘 00:00 ~ 23:59 범위 설정
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);


                verified = verificationRepository.existsTodayVerification(	// 오늘이 인증날이고, 인증을 완료했을 떄에만 true
                        userChallengeId,
                        VerificationStatus.COMPLETED,
                        startOfDay,
                        endOfDay
                );
            }

            // 결과를 DTO 반영
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

        Long currentUserId = user.getId();

        Pageable pageable = PageRequest.of(page, size);

        // DB 조회
        Slice<User> usersSlice = userRepository.findByNicknameContaining(normalize(keyword), user, pageable);

        // DTO 변환을 위한 필터링된 사용자 리스트
        List<User> targetUsers = usersSlice.getContent().stream()
                .filter(target -> !target.getId().equals(currentUserId))
                .toList();

        // N+1 문제 해결을 위한 Bulk 조회
        List<Long> targetIds = targetUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // 현재 사용자가 대상 사용자들을 팔로우하고 있는 Follow 엔티티 리스트를 한 번의 쿼리로 조회
        List<Long> existingFollowIds = followRepository.findFollowingIdsByFollowerIdAndFollowingIds(currentUserId, targetIds);

        // Set 생성: O(1) 시간에 팔로우 여부를 확인하기 위해 Set으로 변환
        Set<Long> followingIdSet = new HashSet<>(existingFollowIds);

        // DTO 변환
        List<UserResponseDto.ProfileDto> profileDtos = targetUsers.stream()
                .map(target -> {
                    // Set을 사용하여 O(1) 복잡도로 팔로우 여부 확인
                    boolean isFollowing = followingIdSet.contains(target.getId());

                    return UserResponseDto.ProfileDto.builder()
                            .userId(target.getId())
                            .profileImage(target.getProfileImage())
                            .nickname(target.getDisplayNickname())
                            .followerCount(null)
                            .followingCount(null)
                            .level(target.getUserLevel())
                            .isFollowing(isFollowing) // Set에서 가져온 값 사용
                            .build();
                })
                .collect(Collectors.toList());

        // SliceResponseDto 생성 및 반환
        Slice<UserResponseDto.ProfileDto> resultSlice = new SliceImpl<>(
                profileDtos,
                pageable,
                usersSlice.hasNext()
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

    // 찜한 챌린지 목록 조회
    @Override
    public SliceResponseDto<UserResponseDto.LikedChallengeDto> getMarkedChallenges(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 찜한 챌린지 조회
        Slice<UserResponseDto.LikedChallengeDto> slice =
                userChallengeRepository.findMarkedChallengesByUser(user, blockedIds, pageable);

        // S3 URL 변환
        slice.getContent().forEach(dto ->
                dto.setImage(s3UrlUtil.toFullUrl(dto.getImage()))
        );

        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(slice);
    }

    // 종료한 챌린지 목록 조회
    @Override
    public SliceResponseDto<UserResponseDto.CompletedChallengeDto> getCompletedChallenges(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 종료한 챌린지 조회
        Slice<UserResponseDto.CompletedChallengeDto> slice =
                userChallengeRepository.findCompletedChallengesByUser(user, pageable);

        // S3 URL 변환
        slice.getContent().forEach(dto ->
                dto.setImage(s3UrlUtil.toFullUrl(dto.getImage()))
        );

        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(slice);
    }

    // 내 정보 수정
    @Override
    @Transactional
    public UpdateUserInfoResponseDto updateUserInfo(Long userId, UpdateUserInfoRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 수정
        if (Boolean.TRUE.equals(requestDto.getIsNicknameChanged())) {
            String rawNickname = requestDto.getNickname();

            // 변경 플래그는 true인데 값이 null인 경우
            if (rawNickname == null || rawNickname.isBlank()) {
                throw new GlobalException(ErrorCode.INVALID_INPUT_VALUE);
            }

            // 값이 있는 경우 업데이트 진행
            String nickname = normalize(rawNickname);

            // 본인의 기존 닉네임과 다른 경우에만 중복 체크 및 수정
            if (!nickname.equals(user.getNickname())) {
                if (userRepository.existsByNickname(nickname)) {
                    throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
                }
                user.updateNickname(nickname);
            }
        }

        // 프로필 이미지 변경 플래그가 true일 때만 처리
        if (Boolean.TRUE.equals(requestDto.getIsProfileImageChanged())) {
            String oldImageKey = user.getProfileImage();
            String newImageKey = requestDto.getProfileImageKey();

            // null이거나 빈 문자열이면 기본 이미지로
            if (newImageKey == null || newImageKey.isBlank()) {
                user.updateProfileImage(null);  // DB에 null 저장

                // 기존 커스텀 이미지가 있으면 트랜잭션 커밋 후 S3에서 삭제
                if (oldImageKey != null && !oldImageKey.isBlank()) {
                    eventPublisher.publishEvent(new ProfileImageDeletedEvent(oldImageKey));
                }
            }
            // 동일 이미지가 아니면 변경
            else if (!newImageKey.equals(oldImageKey)) {
                user.updateProfileImage(newImageKey);

                // 기존 이미지가 있으면 트랜잭션 커밋 후 S3에서 삭제
                if (oldImageKey != null && !oldImageKey.isBlank()) {
                    eventPublisher.publishEvent(new ProfileImageDeletedEvent(oldImageKey));
                }
            }
        }

        // 응답 시 Full URL로 변환
        String profileImageUrl = s3UrlUtil.toFullUrl(user.getProfileImage());

        return UpdateUserInfoResponseDto.from(user, profileImageUrl);
    }
}
