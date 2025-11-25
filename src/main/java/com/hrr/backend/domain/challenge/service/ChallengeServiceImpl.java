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
import com.hrr.backend.domain.challenge.entity.ChallengeLike; // Import 추가
import com.hrr.backend.domain.challenge.entity.ChallengeWait;
import com.hrr.backend.domain.challenge.repository.ChallengeLikeRepository; // Import 추가
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
	private final ChallengeLikeRepository challengeLikeRepository;

	private final UserRepository userRepository;
	private final UserChallengeRepository userChallengeRepository;
	private final UserChallengeConverter userChallengeConverter;

	private final RedisTemplate<String, String> redisTemplate;

	private static final int UPCOMING_DAYS_CRITERIA = 5;

	private static final String TODAY_CHALLENGE_RANKING_KEY = "today:challenge:clicks";
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
		LocalDateTime upcomingStartDate = null;
		LocalDateTime upcomingEndDate = null;

		if (isUpcoming != null && isUpcoming) {
			LocalDate today = LocalDate.now();
			upcomingStartDate = today.atStartOfDay();
			upcomingEndDate = today.plusDays(UPCOMING_DAYS_CRITERIA)
					.atTime(23, 59, 59, 999_999_999);
		}

		Pageable pageable = PageRequest.of(page, size);

		Slice<ChallengeResponseDto.InfoDto> tempDtoSlice = challengeRepository.findChallengesWithFilters(
				category,
				upcomingStartDate,
				upcomingEndDate,
				sortType,
				days,
				title,
				pageable
		);

		List<ChallengeResponseDto.InfoDto> rawContent = tempDtoSlice.getContent();
		List<ChallengeResponseDto.InfoDto> enrichedContent = enrichChallengeInfo(rawContent);

		Slice<ChallengeResponseDto.InfoDto> finalDtoSlice = new SliceImpl<>(
				enrichedContent,
				pageable,
				tempDtoSlice.hasNext()
		);

		return new SliceResponseDto<>(finalDtoSlice);
	}

	@Override
	public Long clickChallenge(Long challengeId) {
		challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		Double updatedScore = redisTemplate.opsForZSet().incrementScore(
				TODAY_CHALLENGE_RANKING_KEY,
				String.valueOf(challengeId),
				1.0
		);

		return (updatedScore != null) ? updatedScore.longValue() : 0L;
	}

	@Override
	public List<ChallengeResponseDto.DailyTopDto> getDailyTopChallenges(int number) {
		long currentListSize = Optional.ofNullable(
				redisTemplate.opsForZSet().size(TODAY_CHALLENGE_RANKING_KEY)).orElse(0L);

		if (currentListSize == 0) {
			return Collections.emptyList();
		}

		if(number > currentListSize){
			number = (int)currentListSize;
		}

		Set<ZSetOperations.TypedTuple<String>> topRankings = redisTemplate.opsForZSet().reverseRangeWithScores(
				TODAY_CHALLENGE_RANKING_KEY,
				0, number-1
		);

		Map<Long, Long> clicksMap = topRankings.stream()
				.collect(Collectors.toMap(
						tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())),
						tuple -> (tuple.getScore() != null) ? tuple.getScore().longValue() : 0L
				));

		List<Long> challengeIds = topRankings.stream()
				.map(tuple -> Long.parseLong(Objects.requireNonNull(tuple.getValue())))
				.toList();

		List<ChallengeResponseDto.InfoDto> rawInfosFromRDB = challengeRepository.findChallengesByIds(challengeIds);

		List<ChallengeResponseDto.InfoDto> enrichedInfos = enrichChallengeInfo(rawInfosFromRDB);

		Map<Long, ChallengeResponseDto.InfoDto> infoMap = enrichedInfos.stream()
				.collect(Collectors.toMap(
						ChallengeResponseDto.InfoDto::getChallengeId,
						Function.identity()
				));

		List<ChallengeResponseDto.DailyTopDto> results = topRankings.stream()
				.map(tuple -> {
					Long id = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
					ChallengeResponseDto.InfoDto info = infoMap.get(id);
					Long clickCount = clicksMap.getOrDefault(id, 0L);

					if (info == null) return null;

					return ChallengeResponseDto.DailyTopDto.builder()
							.clickCount(clickCount)
							.info(info)
							.build();
				})
				.filter(Objects::nonNull)
				.toList();

		for (int i = 0; i < results.size(); i++) {
			results.get(i).setRanking(i + 1);
		}

		return results;
	}

	private List<ChallengeResponseDto.InfoDto> enrichChallengeInfo(List<ChallengeResponseDto.InfoDto> rawInfos) {
		if (rawInfos.isEmpty()) {
			return rawInfos;
		}

		List<Long> challengeIds = rawInfos.stream()
				.map(ChallengeResponseDto.InfoDto::getChallengeId)
				.toList();

		List<ChallengeDayJoin> allDays = challengeDayJoinRepository.findByChallengeIdIn(challengeIds);

		Map<Long, List<ChallengeDays>> daysMap = allDays.stream()
				.collect(Collectors.groupingBy(
						dayJoin -> dayJoin.getChallenge().getId(),
						Collectors.mapping(ChallengeDayJoin::getDayOfWeek, Collectors.toList())
				));

		return rawInfos.stream()
				.map(infoDto -> {
					LocalDate challengeStartDate = infoDto.getStartDate().toLocalDate();
					LocalDate today = LocalDate.now();

					long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
					boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

					infoDto.setIsUpcoming(isUpcomingResult);
					infoDto.setDDayUntilStart((int)Math.max(0, dDay));
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
		validateCreateRequest(req);

		boolean isPublic = req.getIsPublic();
		boolean isViewerMode = isPublic && req.getIsViewerMode();
		String password = isPublic ? null : req.getPassword();

		String imageUrl = req.getImageUrl();
		if (imageUrl == null || imageUrl.isBlank()) {
			imageUrl = DEFAULT_CHALLENGE_IMAGE_URL;
		}

		Challenge challenge = challengeConverter.toChallengeEntity(
				req,
				isPublic,
				isViewerMode,
				password,
				imageUrl
		);
		Challenge saved = challengeRepository.save(challenge);

		UserChallenge userChallenge = userChallengeConverter.toOwner(user, saved);
		userChallengeRepository.save(userChallenge);

		return challengeConverter.toCreateResponseDto(saved);
	}

	@Override
	@Transactional
	public ChallengeResponseDto.JoinChallengeDto joinChallenge(
			User user,
			Long challengeId,
			ChallengeRequestDto.JoinChallengeDto req
	) {
		Challenge challenge = findChallenge(challengeId);

		validateJoinRequest(challenge, user, req.getPassword());

		UserChallenge userChallenge = userChallengeConverter.toChallenger(user, challenge);

		try {
			userChallengeRepository.save(userChallenge);
		} catch (DataIntegrityViolationException e) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}
		challenge.increaseCurrentParticipants();

		return new ChallengeResponseDto.JoinChallengeDto(challenge.getId());
	}

	@Override
	@Transactional
	public void registerChallengeWait(User user, Long challengeId) {
		Challenge challenge = findChallenge(challengeId);

		if (userChallengeRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}

		if (challengeWaitRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_WAIT_ALREADY_EXIST);
		}

		if (challenge.getCurrentParticipants() < challenge.getMaxParticipants()) {
			throw new GlobalException(ErrorCode.CHALLENGE_NOT_FULL);
		}

		ChallengeWait challengeWait = ChallengeWait.builder()
				.user(user)
				.challenge(challenge)
				.build();

		challengeWaitRepository.save(challengeWait);
	}

	@Override
	@Transactional
	public void cancelChallengeWait(User user, Long challengeId) {
		Challenge challenge = findChallenge(challengeId);

		ChallengeWait challengeWait = challengeWaitRepository.findByUserAndChallenge(user, challenge)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_WAIT_NOT_FOUND));

		challengeWaitRepository.delete(challengeWait);
	}

	@Override
	@Transactional
	public ChallengeResponseDto.ChallengeLikeDto likeChallenge(User user, Long challengeId) {

		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// exists 체크 대신 try-catch로 예외 처리
		try {
			ChallengeLike challengeLike = ChallengeLike.builder()
					.user(user)
					.challenge(challenge)
					.build();

			// 이미 존재하면 유니크 제약 조건 위반 예외 발생
			challengeLikeRepository.save(challengeLike);

			// 저장이 성공적으로 된 경우에만 카운트 증가
			challengeRepository.increaseLikeCount(challengeId);

		} catch (DataIntegrityViolationException e) {
			// 이미 좋아요가 눌러져 있는 경우이므로 예외를 무시하고 정상 응답 반환
		}

		// 최신 상태 조회 (좋아요 수 등 갱신된 정보)
		Challenge updatedChallenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		return challengeConverter.toChallengeLikeDto(updatedChallenge, true);
	}

	@Override
	@Transactional
	public ChallengeResponseDto.ChallengeLikeDto unlikeChallenge(User user, Long challengeId) {

		Challenge challenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// 조회(findBy) 없이 바로 삭제 시도
		int deletedCount = challengeLikeRepository.deleteByUserAndChallenge(user, challenge);

		// 실제로 삭제된 데이터가 있을 경우에만(1개) 카운트 감소
		if (deletedCount > 0) {
			challengeRepository.decreaseLikeCount(challengeId);
		}

		Challenge updatedChallenge = challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		return challengeConverter.toChallengeLikeDto(updatedChallenge, false);
	}

	private void validateCreateRequest(ChallengeRequestDto.CreateChallengeDto req) {
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
		if (challenge.getStatus() != ChallengeStatus.UPCOMING) {
			throw new GlobalException(ErrorCode.CHALLENGE_NOT_RECRUITING);
		}

		if (userChallengeRepository.existsByUserAndChallenge(user, challenge)) {
			throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
		}

		if (challenge.getCurrentParticipants() >= challenge.getMaxParticipants()) {
			throw new GlobalException(ErrorCode.CHALLENGE_FULL);
		}

		if (!challenge.getIsPublic()) {
			if (inputPassword == null || !inputPassword.equals(challenge.getPassword())) {
				throw new GlobalException(ErrorCode.CHALLENGE_PASSWORD_MISMATCH);
			}
		}
	}

	private Challenge findChallenge(Long challengeId) {
		return challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
	}
}