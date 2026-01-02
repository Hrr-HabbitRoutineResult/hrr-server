package com.hrr.backend.domain.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.auth.repository.SocialAuthRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;

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

	@Transactional
	public void processPermanentWithdrawal(User user) {
		// Social Auth 정보 삭제
		socialAuthRepository.deleteByUser(user);

		// 개인정보 마스킹 및 삭제
		user.completeWithdrawal();

		// 참여 중인 챌린지 인원수 감소 및 상태 변경
		List<UserChallenge> activeChallenges = userChallengeRepository
			.findByUserAndStatus(user, ChallengeJoinStatus.JOINED);

		for (UserChallenge uc : activeChallenges) {
			uc.updateStatus(ChallengeJoinStatus.DROPPED);

			challengeRepository.decreaseCurrentParticipantCount(uc.getChallenge().getId()); // 인원수 -1
		}

		userRepository.save(user);
	}
}
