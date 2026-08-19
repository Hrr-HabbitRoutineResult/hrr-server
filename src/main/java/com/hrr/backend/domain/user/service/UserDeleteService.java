package com.hrr.backend.domain.user.service;

import java.util.List;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.follow.service.FollowCountService;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.auth.repository.SocialAuthRepository;
import com.hrr.backend.domain.auth.service.AuthService;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.s3.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// UserService는 사용자 중심 로직 담당으로 남기고, 삭제 관련 로직을 분리하여 관리 차원 로직 담당
// 스케줄러에 UserService를 주입하는 것보다는 상대적으로 규모가 작은 해당 클래스를 주입
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeleteService {

	private final UserRepository userRepository;
	private final UserChallengeRepository userChallengeRepository;
	private final ChallengeRepository challengeRepository;
	private final SocialAuthRepository socialAuthRepository;
    private final FollowRepository followRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	private final S3Service s3Service;
	private final AuthService authService;
	private final FollowCountService followCountService;

	/**
	 * 탈퇴 30일 후 완전한 탈퇴 처리
	 * @param user 탈퇴 대상 유저
	 */
	@Transactional
	public void processPermanentWithdrawal(User user) {
		// 소셜 로그인 연결 해제
		try {
			authService.revoke(user.getId());
		} catch (Exception ignored) {
			// 외부 연동 해제 실패가 내부 개인정보 정리를 막지 않도록 계속 진행하되 결과는 추적한다.
			log.warn("[processPermanentWithdrawal] 소셜 연결 해제 실패 후 내부 데이터 정리를 계속합니다. userId={}",
				user.getId());
		}

		// Social Auth 정보 삭제
		socialAuthRepository.deleteByUser(user);

		// S3에서 프로필 이미지 파일 삭제 - 소셜 플랫폼에서 제공된 이미지는 대상에서 제외
		s3Service.deleteFileByKey(user.getProfileImage());

		// 참여 중인 챌린지 인원수 감소 및 상태 변경
		List<UserChallenge> activeChallenges =
				userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED);

		for (UserChallenge uc : activeChallenges) {
			Challenge challenge = uc.getChallenge();

			// 동시성 보장을 위해 챌린지 행 잠금
			Challenge lockedChallenge = challengeRepository.findByIdForUpdate(challenge.getId())
					.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

			boolean wasFull = lockedChallenge.getCurrentParticipants()
					.equals(lockedChallenge.getMaxParticipants());

			uc.updateStatus(ChallengeJoinStatus.KICKED);

			lockedChallenge.decreaseCurrentParticipants();

			// 만석 → 빈자리 전환 시에만 빈자리 이벤트 발행
			if (wasFull) {
				applicationEventPublisher.publishEvent(
						new ChallengeVacancyEvent(lockedChallenge.getId()));
			}
		}

		Long userId = user.getId();

		// 1. 내가 팔로우하던 사람들의 ID를 가져옴
		List<Long> followingIds = followRepository.findAllByFollowerIdAndStatus(userId, FollowStatus.APPROVED)
				.stream().map(f -> f.getFollowing().getId()).toList();

		// 2. 나를 팔로우하던 사람들의 ID를 가져옴
		List<Long> followerIds = followRepository.findAllByFollowingIdAndStatus(userId, FollowStatus.APPROVED)
				.stream().map(f -> f.getFollower().getId()).toList();

		// 3. 관계 삭제 (이걸 먼저 해야 sync할 때 정확한 숫자가 나옴)
		followRepository.deleteAllByUserId(userId);

		// 4. 영향을 받은 모든 유저의 카운트 재계산 (벌크 -1보다 훨씬 안전!)
		followingIds.forEach(followCountService::syncCounts);
		followerIds.forEach(followCountService::syncCounts);

		// 유저 정보 마스킹 및 상태 변경
		user.completeWithdrawal();

		userRepository.save(user);
	}
}
