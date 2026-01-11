package com.hrr.backend.domain.user.service;

import java.util.List;

import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import com.hrr.backend.domain.follow.repository.FollowRepository;
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

	private final S3Service s3Service;
	private final AuthService authService;

	@Transactional
	public void processPermanentWithdrawal(User user) {
		// 소셜 로그인 연결 해제
		try {
			authService.revoke(user.getId());
		} catch (Exception e) {
			log.warn("소셜 연동 해제 실패. 내부 데이터 정리는 계속 진행합니다. userId: {}", user.getId(), e);
		}

		// Social Auth 정보 삭제
		socialAuthRepository.deleteByUser(user);

		// S3에서 프로필 이미지 파일 삭제 - 소셜 플랫폼에서 제공된 이미지는 대상에서 제외
		s3Service.deleteFileByKey(user.getProfileImage());

		// 참여 중인 챌린지 인원수 감소 및 상태 변경
		List<UserChallenge> activeChallenges = userChallengeRepository
			.findByUserAndStatus(user, ChallengeJoinStatus.JOINED);

		for (UserChallenge uc : activeChallenges) {
			uc.updateStatus(ChallengeJoinStatus.DROPPED);

			int updatedRowNumber = challengeRepository.decreaseCurrentParticipantCount(uc.getChallenge().getId()); // 인원수 -1
			if (updatedRowNumber == 0) {
				log.error("일부 챌린지의 현재 참가 인원에 대한 업데이트가 진행되지 않았습니다. UserChallengeId: {}", uc.getId());
			}
		}

        Long userId = user.getId();

        // 내가 팔로우하던 사람들의 팔로워 카운트 벌크 감소
        List<Follow> followings = followRepository.findAllByFollowerIdAndStatus(userId, FollowStatus.APPROVED);
        if (!followings.isEmpty()) {
            List<Long> followingIds = followings.stream()
                    .map(f -> f.getFollowing().getId())
                    .toList();
            userRepository.decrementFollowerCounts(followingIds);
            followRepository.deleteAllInBatch(followings);
        }

        // 나를 팔로우하던 사람들의 팔로잉 카운트 벌크 감소
        List<Follow> followers = followRepository.findAllByFollowingIdAndStatus(userId, FollowStatus.APPROVED);
        if (!followers.isEmpty()) {
            List<Long> followerIds = followers.stream()
                    .map(f -> f.getFollower().getId())
                    .toList();
            userRepository.decrementFollowingCounts(followerIds);
            followRepository.deleteAllInBatch(followers);
        }

        // 유저 정보 마스킹 및 상태 변경
        user.completeWithdrawal();

		userRepository.save(user);
	}
}
