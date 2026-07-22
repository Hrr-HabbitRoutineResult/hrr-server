package com.hrr.backend.domain.challenge.service;


import java.util.List;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.response.SliceResponseDto;

public interface ChallengeService {

	// 챌린지 리스트 조회
	SliceResponseDto<ChallengeResponseDto.InfoDto> getChallengeList(
		Category category,
		Boolean isUpcoming,
		SortType sortType,
		List<ChallengeDays> days,
		String title,
		int page,
		int size
	);

	// 챌린지 상단 헤더 조회
	ChallengeResponseDto.HeaderInfoDto getChallengeHeaderInfo(Long challengeId, User user);

	// 챌린지 프로필 조회
	ChallengeResponseDto.ChallengeProfileDto getChallengeProfile(User user, Long challengeId);

	// 챌린지 클릭 처리
	Long clickChallenge(Long challengeId);

	// 오늘의 인기 챌린지 조회
	List<ChallengeResponseDto.DailyTopDto> getDailyTopChallenges(int number);

	// 챌린지 생성
	ChallengeResponseDto.CreateChallengeDto createChallenge(
			User user,
			ChallengeRequestDto.CreateChallengeDto requestDto
	);

	// 챌린지 참가
	ChallengeResponseDto.JoinChallengeDto joinChallenge(
			User user,
			Long challengeId,
			ChallengeRequestDto.JoinChallengeDto requestDto
	);

	// 챌린지 공석 알림 신청
	void registerChallengeWait(User user, Long challengeId);

	// 챌린지 공석 알림 취소
	void cancelChallengeWait(User user, Long challengeId);

	// 챌린지 좋아요 등록
	ChallengeResponseDto.ChallengeLikeDto likeChallenge(User user, Long challengeId);

	// 챌린지 좋아요 취소
	ChallengeResponseDto.ChallengeLikeDto unlikeChallenge(User user, Long challengeId);

	// 챌린지 라운드 목록 조회
	List<ChallengeResponseDto.RoundDto> getChallengeRounds(Long challengeId);

    // 챌린지 수정 기능
    ChallengeResponseDto.UpdateChallengeDto updateChallenge(
            User user,
            Long challengeId,
            ChallengeRequestDto.UpdateChallengeDto requestDto
    );

    // 챌린지 나가기 (방장 불가, 시작일 전까지만 가능)
    ChallengeResponseDto.LeaveChallengeDto leaveChallenge(User user, Long challengeId);

    // 챌린지 수정 화면 진입 시 폼을 채우기 위한 상세 정보 조회
    ChallengeResponseDto.EditInfoDto getChallengeEditInfo(User user, Long challengeId);
}
