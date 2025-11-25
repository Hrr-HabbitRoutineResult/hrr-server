package com.hrr.backend.domain.challenge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeWait;
import com.hrr.backend.domain.challenge.repository.ChallengeWaitRepository;
import com.hrr.backend.domain.user.converter.UserChallengeConverter;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;
	private final ChallengeDayJoinRepository challengeDayJoinRepository;
	private final ChallengeConverter challengeConverter;

	private final ChallengeWaitRepository challengeWaitRepository;

	private final UserRepository userRepository;
	private final UserChallengeRepository userChallengeRepository;
	private final UserChallengeConverter userChallengeConverter;

	private final RedisTemplate<String, String> redisTemplate;

	private static final int UPCOMING_DAYS_CRITERIA = 5;   // '곧 시작' 챌린지 판단 기준 일자

	// 오늘의 클릭수를 저장할 Redis Key
	private static final String TODAY_CHALLENGE_RANKING_KEY = "today:challenge:clicks";

	// 기본 이미지 URL
	private static final String DEFAULT_CHALLENGE_IMAGE_URL = "https://example.com/images/challenge-default.png";

	@Override
	public SliceResponseDto<ChallengeResponseDto.InfoDto> getChallengeList(
			Category category,
			Boolean isUpcoming,
			SortType sortType,
			List<ChallengeDays> days,
			String title,
			int page,
			int size
	) {
		// isUpcoming에 따라 '곧 시작' 유효날짜 범위 계산
		LocalDateTime upcomingStartDate = null;
		LocalDateTime upcomingEndDate = null;

		if (isUpcoming != null && isUpcoming) {
			LocalDate today = LocalDate.now();

			// 시작일
			upcomingStartDate = today.atStartOfDay();

			// 종료일 ; 오늘 + 5일
			upcomingEndDate = today.plusDays(UPCOMING_DAYS_CRITERIA)
					.atTime(23, 59, 59, 999_999_999);
		}

		Pageable pageable = PageRequest.of(page, size);

		// Repository 호출 - 필터 적용된 챌린지들 반환(요일, 곧 시작 여부, 시작까지 남은 일자는 제외)
		Slice<ChallengeResponseDto.InfoDto> tempDtoSlice = challengeRepository.findChallengesWithFilters(
				category,
				upcomingStartDate,
				upcomingEndDate,
				sortType,
				days,
				title,
				pageable
		);

		// // ---응답 dto에 추가할 요일 정보 조회---
		// // 모든 challengeId 추출
		// List<Long> challengeIds = tempDtoSlice.getContent().stream()
		// 	.map(ChallengeResponseDto.InfoDto::getChallengeId)
		// 	.toList();
		//
		// // 해당 challenge들을 가진 ChallengeDayJoin 엔티티 조회
		// List<ChallengeDayJoin> allDays = challengeDayJoinRepository.findByChallengeIdIn(challengeIds);
		//
		// // 챌린지 아이디-요일 리스트 매핑
		// Map<Long, List<ChallengeDays>> daysMap = allDays.stream()
		// 	.collect(Collectors.groupingBy(
		// 		dayJoin -> dayJoin.getChallenge().getId(), // key: 챌린지 아이디
		// 		Collectors.mapping(ChallengeDayJoin::getDayOfWeek, Collectors.toList()) // value: 요일 리스트
		// 	));
		//
		//
		// // Repository에서 조회해 온 dto 기반으로 결과 dto 필드 일부 채우기
		// Slice<ChallengeResponseDto.InfoDto> finalDtoSlice = tempDtoSlice.map(tempDto -> {
		//
		// 	LocalDate challengeStartDate = tempDto.getStartDate().toLocalDate();
		// 	LocalDate today = LocalDate.now();
		//
		// 	long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
		// 	boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);
		//
		// 	// dto 나머지 필드 채우기(isUpcoming, dDayUntilStart, dayOfWeek)
		// 	tempDto.setIsUpcoming(isUpcomingResult);
		// 	tempDto.setDDayUntilStart((int)Math.max(0, dDay));	// 시작 전일 경우에만 남은 날짜를 set, else 0
		// 	tempDto.setDaysOfWeek(daysMap.getOrDefault(tempDto.getChallengeId(), List.of()));
		//
		// 	return tempDto;
		// });

		// dto 나머지 필드 보강 (isUpcoming, dDayUntilStart, dayOfWeek)
		List<ChallengeResponseDto.InfoDto> rawContent = tempDtoSlice.getContent();
		List<ChallengeResponseDto.InfoDto> enrichedContent = enrichChallengeInfo(rawContent);

		Slice<ChallengeResponseDto.InfoDto> finalDtoSlice = new SliceImpl<>(
				enrichedContent,
				pageable,
				tempDtoSlice.hasNext()
		);

		return new SliceResponseDto<>(finalDtoSlice);
	}

	// @Override
	// public Integer getChallengeProfile(Long challengeId) {
	//
	// 	// TODO: 챌린지 프로필 조회 구현
	//
	// 	return Math.toIntExact((updatedScore != null) ? updatedScore.longValue() : 0L);
	// }

	@Override
	public Long clickChallenge(Long challengeId) {
		// 챌린지 유효성 검사
		challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// 클릭 수 증가 로직
		Double updatedScore = redisTemplate.opsForZSet().incrementScore(
				TODAY_CHALLENGE_RANKING_KEY,
				String.valueOf(challengeId), // 챌린지 아이디를 key로 사용
				1.0 // 클릭 수 1.0 증가 (redis sorted set의 메서드 정의 상 double 타입 필요)
		);

		return (updatedScore != null) ? updatedScore.longValue() : 0L;
	}

	@Override
	public List<ChallengeResponseDto.DailyTopDto> getDailyTopChallenges(int number) {

		// 현재 목록 개수 범위를 넘어서면 있는 만큼 반환(UX 고려)
		long currentListSize = Optional.ofNullable(
				redisTemplate.opsForZSet().size(TODAY_CHALLENGE_RANKING_KEY)).orElse(0L);

		// 데이터가 없을 경우 (아마 00시 직후) 빈 리스트 반환
		if (currentListSize == 0) {
			return Collections.emptyList();
		}

		if(number > currentListSize){
			number = (int)currentListSize;
		}

		// Top N 조회
		Set<ZSetOperations.TypedTuple<String>> topRankings = redisTemplate.opsForZSet().reverseRangeWithScores(
				TODAY_CHALLENGE_RANKING_KEY,
				0, number-1
		);

		// <챌린지 id, 클릭 수> 로딩
		Map<Long, Long> clicksMap = topRankings.stream()
				.collect(Collectors.toMap(
						tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())),
						tuple -> (tuple.getScore() != null) ? tuple.getScore().longValue() : 0L
				));

		// 해당 챌린지들 DB에서 조회
		List<Long> challengeIds = topRankings.stream()
				.map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())))
				.toList();

		List<ChallengeResponseDto.InfoDto> rawInfosFromRDB = challengeRepository.findChallengesByIds(challengeIds);

		// D-Day, isUpcoming, daysOfWeek 추가
		List<ChallengeResponseDto.InfoDto> enrichedInfos = enrichChallengeInfo(rawInfosFromRDB);

		Map<Long, ChallengeResponseDto.InfoDto> infoMap = enrichedInfos.stream()
				.collect(Collectors.toMap(
						ChallengeResponseDto.InfoDto::getChallengeId,
						Function.identity() // Identity function: InfoDto 객체 자체를 값으로 사용
				));

		// 최종 DTO
		List<ChallengeResponseDto.DailyTopDto> results = topRankings.stream()
				.map(tuple -> {
					Long id = Long.parseLong(Objects.requireNonNull(tuple.getValue()));

					// DB에서 받아온 챌린지 정보와 클릭 수 조회
					ChallengeResponseDto.InfoDto info = infoMap.get(id);
					Long clickCount = clicksMap.getOrDefault(id, 0L);

					// 챌린지 정보가 조회되지 않으면 패스
					if (info == null) return null;

					return ChallengeResponseDto.DailyTopDto.builder()
							.clickCount(clickCount)
							.info(info)
							.build();
				})
				.filter(Objects::nonNull)
				.toList();

		// 랭킹 정보 추가
		for (int i = 0; i < results.size(); i++) {
			results.get(i).setRanking(i + 1);
		}

		return results;
	}

	/**
	 * DB에서 조회해 온 Challenge InfoDto에 누락되어있는 D-Day, isUpcoming, daysOfWeek 정보를 추가
	 * @param rawInfos DB에서 조회된 ChallengeResponseDto.InfoDto 리스트
	 */
	private List<ChallengeResponseDto.InfoDto> enrichChallengeInfo(List<ChallengeResponseDto.InfoDto> rawInfos) {

		if (rawInfos.isEmpty()) {
			return rawInfos;
		}

		// ---응답 dto에 추가할 요일 정보 조회---
		// 모든 challengeId 추출
		List<Long> challengeIds = rawInfos.stream()
				.map(ChallengeResponseDto.InfoDto::getChallengeId)
				.toList();

		// 해당 challenge들을 가진 ChallengeDayJoin 엔티티 조회
		List<ChallengeDayJoin> allDays = challengeDayJoinRepository.findByChallengeIdIn(challengeIds);

		// 챌린지 아이디-요일 리스트 매핑
		Map<Long, List<ChallengeDays>> daysMap = allDays.stream()
				.collect(Collectors.groupingBy(
						dayJoin -> dayJoin.getChallenge().getId(), // key: 챌린지 아이디
						Collectors.mapping(ChallengeDayJoin::getDayOfWeek, Collectors.toList()) // value: 요일 리스트
				));

		// Repository에서 조회해 온 dto 기반으로 결과 dto 필드 일부 채우기
		return rawInfos.stream()
				.map(infoDto -> {
					LocalDate challengeStartDate = infoDto.getStartDate().toLocalDate();
					LocalDate today = LocalDate.now();

					long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
					boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

					// dto 나머지 필드 채우기 (isUpcoming, dDayUntilStart, dayOfWeek)
					infoDto.setIsUpcoming(isUpcomingResult);
					infoDto.setDDayUntilStart((int)Math.max(0, dDay)); // 시작 전일 경우에만 남은 날짜를 set, else 0
					infoDto.setDaysOfWeek(daysMap.getOrDefault(infoDto.getChallengeId(), List.of()));

					return infoDto;
				}).toList();
	}

	@Override
	@Transactional
	public ChallengeResponseDto.CreateChallengeDto createChallenge(
			User user,
			ChallengeRequestDto.CreateChallengeDto req
	) {
		// 비즈니스 룰 검증
		validateCreateRequest(req);

		boolean isPublic = req.getIsPublic();
		boolean isViewerMode = isPublic && req.getIsViewerMode();
		String password = isPublic ? null : req.getPassword();

		String imageUrl = req.getImageUrl();
		if (imageUrl == null || imageUrl.isBlank()) {
			imageUrl = DEFAULT_CHALLENGE_IMAGE_URL;
		}

		// 챌린지 엔티티 생성
		Challenge challenge = challengeConverter.toChallengeEntity(
				req,
				isPublic,
				isViewerMode,
				password,
				imageUrl
		);
		Challenge saved = challengeRepository.save(challenge);

		// UserChallenge 생성
		UserChallenge userChallenge = userChallengeConverter.toOwner(user, saved);
		userChallengeRepository.save(userChallenge);

		// 응답 반환
		return challengeConverter.toCreateResponseDto(saved);
	}

	@Override
	@Transactional
	public ChallengeResponseDto.JoinChallengeDto joinChallenge(
			User user,
			Long challengeId,
			ChallengeRequestDto.JoinChallengeDto req
	) {
		// 챌린지 조회
		Challenge challenge = findChallenge(challengeId);

		// 비즈니스 룰 검증
		validateJoinRequest(challenge, user, req.getPassword());

		// 참가 처리
		UserChallenge userChallenge = userChallengeConverter.toChallenger(user, challenge);

		try {
			userChallengeRepository.save(userChallenge);
		} catch (DataIntegrityViolationException e) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}
		// 챌린지 인원 업데이트
		challenge.increaseCurrentParticipants();

		return new ChallengeResponseDto.JoinChallengeDto(challenge.getId());
	}

	@Override
	@Transactional
	public void registerChallengeWait(User user, Long challengeId) {

		// 챌린지 조회
		Challenge challenge = findChallenge(challengeId);

		// 챌린지 참여 여부 확인
		if (userChallengeRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}

		// 이미 신청했는지 확인
		if (challengeWaitRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_WAIT_ALREADY_EXIST);
		}

		// 자리가 남아있는 경우 대기 신청 불가
		if (challenge.getCurrentParticipants() < challenge.getMaxParticipants()) {
			throw new GlobalException(ErrorCode.CHALLENGE_NOT_FULL);
		}

		// 저장
		ChallengeWait challengeWait = ChallengeWait.builder()
				.user(user)
				.challenge(challenge)
				.build();

		challengeWaitRepository.save(challengeWait);

	}

	@Override
	@Transactional
	public void cancelChallengeWait(User user, Long challengeId) {

		// 챌린지 조회
		Challenge challenge = findChallenge(challengeId);

		// 대기 내역 조회
		ChallengeWait challengeWait = challengeWaitRepository.findByUserAndChallenge(user, challenge)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_WAIT_NOT_FOUND));

		// 삭제
		challengeWaitRepository.delete(challengeWait);
	}

	/**
	 * 챌린지 생성 요청에 대한 비즈니스 검증 로직
	 */
	private void validateCreateRequest(ChallengeRequestDto.CreateChallengeDto req) {
	@Override
	@Transactional
	public ChallengeResponseDto.ChallengeLikeDto likeChallenge(Long userId, Long challengeId) {
		// 엔티티 조회 (이전 PR 머지 후 수정)
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND));

		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// 중복 좋아요 방지 및 저장
		if (!challengeLikeRepository.existsByUserAndChallenge(user, challenge)) {
			ChallengeLike challengeLike = ChallengeLike.builder()
					.user(user)
					.challenge(challenge)
					.build();
			challengeLikeRepository.save(challengeLike);
			challengeRepository.increaseLikeCount(challengeId);
		}

		// 최신 상태 조회
		Challenge updatedChallenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// DTO 변환
		return challengeConverter.toChallengeLikeDto(updatedChallenge, true);
	}

	@Override
	@Transactional
	public ChallengeResponseDto.ChallengeLikeDto unlikeChallenge(Long userId, Long challengeId) {
		// 엔티티 조회
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new GlobalException(ErrorCode.AUTH_USER_NOT_FOUND));

		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		Optional<ChallengeLike> challengeLike = challengeLikeRepository.findByUserAndChallenge(user, challenge);

		// 좋아요 취소
		if (challengeLike.isPresent()) {
			challengeLikeRepository.deleteByUserAndChallenge(user, challenge);
			challengeRepository.decreaseLikeCount(challengeId);
		}

		// 최신 상태 조회
		Challenge updatedChallenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// DTO 변환
		return challengeConverter.toChallengeLikeDto(updatedChallenge, false);
	}

	private void validateBusinessRules(ChallengeRequestDto.CreateChallengeDto req) {
		// 인증 시간 검증: 종료 시간 > 시작 시간
		if (!req.getVerifyEndTime().isAfter(req.getVerifyStartTime())) {
			throw new GlobalException(ErrorCode.CHALLENGE_INVALID_VERIFY_TIME);
		}

		// 공개/비공개 및 비밀번호, 관찰자 모드 검증
		if (!req.getIsPublic()) {
			// 비공개인데 비밀번호가 없거나 4자리 숫자가 아닌 경우
			if (req.getPassword() == null || !req.getPassword().matches("^\\d{4}$")) {
				throw new GlobalException(ErrorCode.CHALLENGE_PRIVATE_PASSWORD_REQUIRED);
			}

			// 비공개 챌린지인데 관찰자 모드를 설정한 경우
			if (req.getIsViewerMode()) {
				throw new GlobalException(ErrorCode.CHALLENGE_PRIVATE_VIEWER_MODE_NOT_ALLOWED);
			}
		} else {
			// 공개인데 비밀번호가 입력된 경우
			if (req.getPassword() != null && !req.getPassword().isBlank()) {
				throw new GlobalException(ErrorCode.CHALLENGE_PUBLIC_PASSWORD_INPUT);
			}
		}
	}

	/**
	 * 챌린지 참가 요청에 대한 비즈니스 검증 로직
	 */
	private void validateJoinRequest(Challenge challenge, User user, String inputPassword) {
		// 모집 상태 검증 (UPCOMING 상태인지)
		if (challenge.getStatus() != ChallengeStatus.UPCOMING) {
			throw new GlobalException(ErrorCode.CHALLENGE_NOT_RECRUITING);
		}

		// 중복 참가 검증
		if (userChallengeRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}

		// 정원 초과 검증
		if (challenge.getCurrentParticipants() >= challenge.getMaxParticipants()) {
			throw new GlobalException(ErrorCode.CHALLENGE_FULL);
		}

		// 비밀번호 검증 (비공개 챌린지인 경우)
		if (!challenge.getIsPublic()) {
			if (inputPassword == null || !inputPassword.equals(challenge.getPassword())) {
				throw new GlobalException(ErrorCode.CHALLENGE_PASSWORD_MISMATCH);
			}
		}
	}

	// 챌린지 조회 헬퍼 메서드
	private Challenge findChallenge(Long challengeId) {
		return challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
	}

}