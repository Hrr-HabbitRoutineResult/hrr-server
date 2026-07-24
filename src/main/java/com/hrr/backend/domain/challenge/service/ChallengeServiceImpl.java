package com.hrr.backend.domain.challenge.service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
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
import com.hrr.backend.domain.challenge.event.ChallengeCreatedEvent;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.repository.ChallengeLikeRepository; // Import 추가
import com.hrr.backend.domain.challenge.repository.ChallengeWaitRepository;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.round.converter.RoundConverter;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.converter.UserChallengeConverter;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

@Slf4j
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

	private final RoundRepository roundRepository;
	private final RoundRecordRepository roundRecordRepository;
	private final RoundConverter roundConverter;

	private final ChallengeStaticsService challengeStaticsService;

	private final VerificationRepository verificationRepository;

	private final RedisTemplate<String, String> redisTemplate;

    private final ApplicationEventPublisher eventPublisher;


    private static final int UPCOMING_DAYS_CRITERIA = 5;	// '곧 시작' 챌린지 판단 기준 일자

	// 오늘의 클릭수를 저장할 Redis Key
	private static final String TODAY_CHALLENGE_RANKING_KEY = "today:challenge:clicks";

	private final S3UrlUtil s3UrlUtil;

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

			// 시작일 ; 오늘 + 1일
			upcomingStartDate = today.atStartOfDay().plusDays(1);

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

	@Override
    @Transactional(readOnly = true)
	public ChallengeResponseDto.HeaderInfoDto getChallengeHeaderInfo(Long challengeId, User user) {
		// 챌린지 조회
        Challenge challenge = findChallengeWithDays(challengeId);

		// 라운드 및 날짜 계산
		Round currentRound = challenge.getCurrentRound();
		LocalDate startDate = (currentRound != null) ? currentRound.getStartDate() : null;
		LocalDate endDate = (currentRound != null) ? currentRound.getEndDate() : null;
		long remainDays = calculateRemainDays(endDate);

		// 유저 상태 확인 (참여 여부, 오늘 인증 여부)
		boolean isParticipant = false;
		boolean isCertifiedToday = false;
		boolean isKicked = false; // 강퇴 여부 플래그 추가

        // 유저가 참여 중인지 확인
        Optional<UserChallenge> ucOp = userChallengeRepository.findByUserAndChallenge(user, challenge);

		if (ucOp.isPresent()) {
			UserChallenge uc = ucOp.get();
			// 챌린지가 진행 중일 때만 오늘 인증 여부를 체크함
			if (uc.getStatus() == ChallengeJoinStatus.JOINED && challenge.getStatus() != ChallengeStatus.FINISHED) {
				isParticipant = true;
				isCertifiedToday = checkTodayVerification(uc.getId(), challenge);
			}
			else if (uc.getStatus() == ChallengeJoinStatus.KICKED) {
				isKicked = true;
			}
		}

		// 챌린지 참여 개수 조회
		boolean isMaxJoined = challengeRepository.countByUserIdAndStatus(user.getId(), ChallengeJoinStatus.JOINED) >= 5;

		// 좋아요 여부 조회
		boolean isLiked = challengeLikeRepository.existsByUserAndChallenge(user, challenge);

		// 방장 정보 조회
		UserChallenge ownerUc = userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER)
				.orElse(null);

		// 방장 객체 및 활성 상태 추출
		User owner = (ownerUc != null) ? ownerUc.getUser() : null;
		boolean isOwnerActive = (owner != null) && (owner.getUserStatus() == UserStatus.ACTIVE);

		// 버튼 상태 결정
		ActionButtonStatus buttonStatus = resolveButtonStatus(challenge, isParticipant, isCertifiedToday, isMaxJoined, isKicked);

        // 현재 유저가 방장인지 여부
        boolean isOwner = Objects.equals(owner != null ? owner.getId() : null, user.getId());

		// DTO 변환 및 반환
		return challengeConverter.toHeaderInfoDto(
				challenge, owner, isOwnerActive, startDate, endDate, remainDays,
				isParticipant, isLiked, buttonStatus,
                isOwner
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ChallengeResponseDto.ChallengeProfileDto getChallengeProfile(User user, Long challengeId) {
		// 챌린지 조회
		Challenge challenge = findChallengeWithDays(challengeId);

		// 참여 여부 확인
		boolean isParticipating = userChallengeRepository.findByUserAndChallenge(user, challenge)
				.map(uc -> uc.getStatus() == ChallengeJoinStatus.JOINED)
				.orElse(false);

		List<ChallengeDays> verifiedDaysThisWeek = null;

		// 참여 중일 때만 계산
		if (isParticipating) {
			LocalDateTime now = LocalDateTime.now();

			// 일요일 시작 ~ 토요일 종료 기준으로 범위 설정
			LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MIN);
			LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).with(LocalTime.MAX);

			// DB 조회
			List<com.hrr.backend.domain.verification.entity.Verification> verifications = verificationRepository.findWeeklyVerifications(
					user.getId(),
					challengeId,
					startOfWeek,
					endOfWeek,
					VerificationStatus.COMPLETED
			);

			// Enum 메서드 사용
			verifiedDaysThisWeek = verifications.stream()
					.map(v -> ChallengeDays.from(v.getCreatedAt().getDayOfWeek()))
					.distinct()
					.toList();
		}

		return challengeConverter.toProfileDto(challenge, isParticipating, verifiedDaysThisWeek);
	}

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

		List<ChallengeResponseDto.InfoDto> rawInfosFromRDB = challengeRepository.findNotFinishedChallengesByIds(challengeIds);

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

					if (challengeStartDate == null) {
						// 시작일 없는 챌린지는 Upcoming 대상 아님
						infoDto.setIsUpcoming(false);
						infoDto.setDDayUntilStart(0);
					} else {
						long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
						boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

						// dto 나머지 필드 채우기 (isUpcoming, dDayUntilStart)
						infoDto.setIsUpcoming(isUpcomingResult);
						infoDto.setDDayUntilStart((int)Math.max(0, dDay)); // 시작 전일 경우에만 남은 날짜를 set, else 0
					}


					long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
					boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

					// dto 나머지 필드 채우기 (ThumbnailUrl, dayOfWeek)
					infoDto.setThumbnailUrl(s3UrlUtil.toFullUrl(infoDto.getThumbnailUrl()));
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
		validateCreateRequest(user, req);

		boolean isPublic = req.getIsPublic();
		boolean isViewerMode = isPublic && req.getIsViewerMode();
		String password = isPublic ? null : req.getPassword();

		// Challenge 생성
		Challenge challenge = challengeConverter.toChallengeEntity(
				req,
				isPublic,
				isViewerMode,
				password
		);
		Challenge saved = challengeRepository.save(challenge);

		// Round 생성
		Round firstRound = roundConverter.toFirstRoundEntity(
				saved,
				req.getStartDate()
		);
		roundRepository.save(firstRound);

		// 챌린지의 currentRound 설정
		saved.changeCurrentRound(firstRound);

		// UserChallenge(방장) 생성
		UserChallenge userChallenge = userChallengeConverter.toOwner(user, saved);
		userChallengeRepository.save(userChallenge);

		// RoundRecord(방장의 레코드) 생성
		createRoundRecordOrFail(saved, userChallenge);

		// ChallengeStatics(참가자들의 선호 통계) 생성
		challengeStaticsService.updateChallengeStatics(saved);

        // 임베딩 계산
        String challengeText = buildChallengeText(saved);
        eventPublisher.publishEvent(
                new ChallengeCreatedEvent(saved.getId(), challengeText)
        );


        return challengeConverter.toCreateResponseDto(saved);
	}
    private String buildChallengeText(Challenge challenge) {
        return String.join(" ",
                challenge.getTitle(),
                Objects.toString(challenge.getDescription(), ""),
                Objects.toString(challenge.getRule(), "")
        ).replaceAll("\\s+", " ").trim();
    }

	@Override
	@Transactional
	public ChallengeResponseDto.JoinChallengeDto joinChallenge(
			User user,
			Long challengeId,
			ChallengeRequestDto.JoinChallengeDto req
	) {
		// 정원 초과 방지를 위한 락 조회
		Challenge challenge = findChallengeForUpdate(challengeId);

		// 비즈니스 룰 검증
		validateJoinRequest(challenge, user, req.getPassword());

		// 기존 참여 기록이 있는지 확인 (유니크 제약 조건 충돌 방지)
		Optional<UserChallenge> existingUcOp = userChallengeRepository.findByUserAndChallenge(user, challenge);

		UserChallenge userChallenge;
		if (existingUcOp.isPresent()) {
			// 기존 기록이 있다면 (validateJoinRequest를 통과했으므로 CANCELLED/DROPPED 상태임) 상태만 JOINED로 변경
			userChallenge = existingUcOp.get();
			userChallenge.updateStatus(ChallengeJoinStatus.JOINED);
		} else {
			// 기록이 없다면 신규 생성
			userChallenge = userChallengeConverter.toChallenger(user, challenge);
			userChallengeRepository.save(userChallenge);
		}

		// 챌린지 인원 업데이트
		challenge.increaseCurrentParticipants();

		// 현재 진행 중인 라운드의 RoundRecord 생성
		createRoundRecordOrFail(challenge, userChallenge);

        // challenge_statics 업데이트 트리거
        challengeStaticsService.updateChallengeStatics(challenge);

        // 현재 인원 포함한 응답
		return challengeConverter.toJoinResponseDto(challenge);
	}

    // 챌린지 나가기
    @Override
    @Transactional
    public ChallengeResponseDto.LeaveChallengeDto leaveChallenge(User user, Long challengeId) {
        // 참가자 수 정합성 보장을 위한 락 조회 (참가 로직과 동일)
        Challenge challenge = findChallengeForUpdate(challengeId);

        // 내 참여 정보 조회
        UserChallenge userChallenge = userChallengeRepository.findByUserAndChallenge(user, challenge)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        // 비즈니스 룰 검증 (방장 여부, 참여 상태, 시작일 전 여부)
        validateLeaveRequest(challenge, userChallenge);

        boolean wasFull = challenge.getCurrentParticipants() >= challenge.getMaxParticipants();

        // 현재 라운드의 내 RoundRecord 삭제 (아직 시작 전이라 라운드는 1개, 인증 기록도 없음)
        Round currentRound = challenge.getCurrentRound();
        if (currentRound != null) {
            roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, currentRound.getId())
                    .ifPresent(roundRecordRepository::delete);
        }

        // UserChallenge 이력은 유지하고 시작 전 나가기 상태로 변경
        userChallenge.updateStatus(ChallengeJoinStatus.CANCELLED);

        // 참가자 수 감소 (참가 로직의 반대)
        challenge.decreaseCurrentParticipants();

        // challenge_statics 재집계 (JOINED 기준 전체 재계산이라 join과 동일하게 재사용 가능)
        challengeStaticsService.updateChallengeStatics(challenge);

        if (wasFull && challenge.getCurrentParticipants() < challenge.getMaxParticipants()) {
            eventPublisher.publishEvent(new ChallengeVacancyEvent(challenge.getId()));
        }

        return challengeConverter.toLeaveResponseDto(challenge);
    }

    /**
     * 챌린지 나가기 요청에 대한 비즈니스 검증 로직
     * - 참여 상태(JOINED)가 아니면 나가기 불가
     * - 방장(OWNER)은 나가기 불가
     * - 챌린지가 시작 전(UPCOMING)일 때만 나가기 가능
     */
    private void validateLeaveRequest(Challenge challenge, UserChallenge userChallenge) {

        // 참여 중(JOINED) 상태가 아니면 나가기 불가
        if (userChallenge.getStatus() != ChallengeJoinStatus.JOINED) {
            throw new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND);
        }

        // 방장은 나가기 불가 (수정/삭제만 가능)
        if (userChallenge.getRole() == UserChallengeRole.OWNER) {
            throw new GlobalException(ErrorCode.CHALLENGE_LEAVE_FORBIDDEN_OWNER);
        }

        validateBeforeStartDate(challenge, ErrorCode.CHALLENGE_LEAVE_PERIOD_EXPIRED);
    }

	private void createRoundRecordOrFail(Challenge challenge, UserChallenge userChallenge) {
		Round currentRound = challenge.getCurrentRound(); //

		// 챌린지가 있는데 라운드가 없는 건 서버 에러
		if (currentRound == null) {
			throw new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR); //
		}

		// 이미 기록이 존재하는지 체크
		boolean isExist = roundRecordRepository.existsByUserChallengeAndRound(userChallenge, currentRound);

		if (!isExist) {
			// 기록이 없을 때만 신규 생성
			RoundRecord roundRecord = roundConverter.toRoundRecordEntity(
					currentRound,
					userChallenge
			);
			roundRecordRepository.save(roundRecord);
		}
	}

	@Override
	@Transactional
	public void registerChallengeWait(User user, Long challengeId) {

		// 챌린지 조회
		Challenge challenge = findChallenge(challengeId);

		// 챌린지 참여 여부 확인
		if (userChallengeRepository.existsByUserAndChallengeAndStatus(user, challenge, ChallengeJoinStatus.JOINED)) {
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

	@Override
	@Transactional(readOnly = true)
	public List<ChallengeResponseDto.RoundDto> getChallengeRounds(Long challengeId) {
		// 챌린지 조회
		Challenge challenge = findChallenge(challengeId);

		// 라운드 목록 조회
		List<Round> rounds = roundRepository.findAllByChallengeIdOrderByRoundNumberAsc(challengeId);

		// 현재 라운드 ID 추출 (Null Safe)
		Long currentRoundId = (challenge.getCurrentRound() != null)
				? challenge.getCurrentRound().getId()
				: -1L;

		// 변환
		return rounds.stream()
				.map(round -> {
					boolean isCurrent = round.getId().equals(currentRoundId);
					return challengeConverter.toRoundDto(round, isCurrent);
				})
				.toList();
	}

    // 챌린지 수정용 상세 정보 조회
    @Override
    @Transactional(readOnly = true)
    public ChallengeResponseDto.EditInfoDto getChallengeEditInfo(User user, Long challengeId) {
        // 챌린지 조회 (요일 정보 포함)
        Challenge challenge = findChallengeWithDays(challengeId);

        // 방장 권한 검증 (updateChallenge와 동일한 검증 재사용)
        validateOwner(user, challengeId);

        // 수정 가능 기간 검증 (한국 시간 기준: 시작일 전날 23:59:59까지, updateChallenge와 동일한 검증 재사용)
        validateUpdatePeriod(challenge);

        return challengeConverter.toEditInfoDto(challenge);
    }

    @Override
    @Transactional
    public ChallengeResponseDto.UpdateChallengeDto updateChallenge(
            User user,
            Long challengeId,
            ChallengeRequestDto.UpdateChallengeDto req
    ) {
        // 챌린지 조회
        Challenge challenge = findChallengeWithDays(challengeId);

        // 방장 권한 검증
        validateOwner(user, challengeId);

        // 수정 가능 기간 검증 (한국 시간 기준: 시작일 전날 23:59:59까지)
        validateUpdatePeriod(challenge);

        // 수정 요청에 대한 비즈니스 검증
        validateUpdateRequest(challenge, req);

        // 공개/비공개 처리
        boolean isPublic = req.getIsPublic();
        boolean isViewerMode = isPublic && req.getIsViewerMode();
        String password = resolveUpdatedPassword(challenge, req);

        // Challenge 필드 업데이트
        challenge.update(
                req.getTitle(),
                req.getDescription(),
                isPublic,
                password,
                isViewerMode,
                req.getMaxParticipants(),
                req.getRule(),
                req.getVerifyStartTime(),
                req.getVerifyEndTime(),
                req.getStartDate().atStartOfDay(),
                req.getVerificationType(),
                req.getCategory(),
                req.getImageKey()
        );

        // ChallengeDayJoin 전체 교체 (기존 삭제 후 새로 insert)
        challengeDayJoinRepository.deleteAllByChallenge(challenge);
        List<ChallengeDayJoin> newDays = req.getDaysOfWeek().stream()
                .distinct()
                .map(day -> ChallengeDayJoin.builder()
                        .challenge(challenge)
                        .dayOfWeek(day)
                        .build())
                .toList();
        challengeDayJoinRepository.saveAll(newDays);

        // Round의 startDate / endDate도 갱신 (1라운드 시작일 변경)
        Round currentRound = challenge.getCurrentRound();
        if (currentRound != null) {
            if (req.getStartDate() == null) {
                throw new GlobalException(ErrorCode.CHALLENGE_INVALID_START_DATE);
            }
            currentRound.updateStartDate(req.getStartDate());
        }

        // 수정 후 임베딩/검색 파생 데이터 갱신
        // title, description, rule 변경 시 검색/추천 임베딩이 수정 전 텍스트로 남는 문제 방지
        // createChallenge()와 동일한 이벤트 발행 패턴으로 임베딩 재계산 트리거
        String updatedChallengeText = buildChallengeText(challenge);
        eventPublisher.publishEvent(
                new ChallengeCreatedEvent(challenge.getId(), updatedChallengeText)
        );

		// 챌린지 수정이 완료된 후 참가자 알림 생성을 위해 이벤트 발행
        eventPublisher.publishEvent(new ChallengeUpdatedEvent(challenge.getId()));

        return challengeConverter.toUpdateResponseDto(challenge);
    }

    /**
     * 챌린지 수정 기능 - 방장 권한 검증
     * 해당 챌린지의 OWNER인지 확인, 방장이 아니면 CHALLENGE_UPDATE_FORBIDDEN(403) 예외 발생
     */
    private void validateOwner(User user, Long challengeId) {
        UserChallenge ownerUc = userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        if (!ownerUc.getUser().getId().equals(user.getId())) {
            throw new GlobalException(ErrorCode.CHALLENGE_UPDATE_FORBIDDEN);
        }
    }

    /**
     * 챌린지 수정 기능 - 수정 가능 기간 검증
     * - 한국 시간(KST, Asia/Seoul) 기준
     * - 챌린지 시작일 전날 23:59:59까지만 수정 가능, 시작일 당일부터 CHALLENGE_UPDATE_PERIOD_EXPIRED(400) 예외 발생
     */
    private void validateUpdatePeriod(Challenge challenge) {
        // 한국 시간 기준 현재 날짜
        validateBeforeStartDate(challenge, ErrorCode.CHALLENGE_UPDATE_PERIOD_EXPIRED);
    }

    /**
     * 챌린지 시작일 전(KST 기준)인지 검증하는 공통 헬퍼
     * - 수정(update), 나가기(leave) 등 "챌린지 시작일 전까지만 가능한" 액션에서 공통으로 사용
     * - challenge.getStatus()(UPCOMING/ONGOING 등)는 ChallengeScheduler의 일배치(자정)로만 갱신되어
     *   실제 날짜와 시간차가 생길 수 있으므로, 상태값 대신 KST 날짜를 startDate와 직접 비교한다.
     * - 한국 시간(KST, Asia/Seoul) 기준, 시작일 전날 23:59:59까지만 허용, 시작일 당일부터 예외 발생
     */
    private void validateBeforeStartDate(Challenge challenge, ErrorCode errorCode) {
        // 한국 시간 기준 현재 날짜
        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 챌린지 시작일 (startDate는 LocalDateTime으로 저장되어 있으므로 toLocalDate() 변환)
        LocalDate startDate = challenge.getStartDate().toLocalDate();

        // 시작일 당일 또는 이후라면 예외 발생
        if (!todayKst.isBefore(startDate)) {
            throw new GlobalException(errorCode);
        }
    }

    /**
     * 챌린지 수정 기능 - 수정 요청 비즈니스 검증 로직
     * - 새 시작일이 내일(KST) 이후인지
     * - 인증 종료시간 > 시작시간인지
     * - 최대 참여 인원 >= 현재 참여 인원인지
     * - 공개/비공개 비밀번호 및 관찰자 모드 유효성
     */
    private void validateUpdateRequest(Challenge challenge, ChallengeRequestDto.UpdateChallengeDto req) {

        // 시작일 검증: 내일(KST) 이후여야 함
        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (!req.getStartDate().isAfter(todayKst)) {
            throw new GlobalException(ErrorCode.CHALLENGE_INVALID_START_DATE);
        }

        // 인증 시간 검증: 종료 시간 > 시작 시간
        if (!req.getVerifyEndTime().isAfter(req.getVerifyStartTime())) {
            throw new GlobalException(ErrorCode.CHALLENGE_INVALID_VERIFY_TIME);
        }

        // 최대 참여 인원이 현재 참여 인원보다 작으면 안 됨
        if (req.getMaxParticipants() < challenge.getCurrentParticipants()) {
            throw new GlobalException(ErrorCode.CHALLENGE_MAX_PARTICIPANTS_BELOW_CURRENT);
        }

        // 공개/비공개 및 비밀번호, 관찰자 모드 검증
        if (!req.getIsPublic()) {
            boolean isPasswordProvided = req.getPassword() != null && !req.getPassword().isBlank();
            boolean hadPasswordBefore = !challenge.getIsPublic()
                    && challenge.getPassword() != null
                    && !challenge.getPassword().isBlank();

            if (isPasswordProvided) {
                // 새 비밀번호를 입력한 경우 형식 검증 (4자리 숫자)
                if (!req.getPassword().matches("^\\d{4}$")) {
                    throw new GlobalException(ErrorCode.CHALLENGE_PRIVATE_PASSWORD_REQUIRED);
                }
            } else if (!hadPasswordBefore) {
                // 미입력인데 기존 비밀번호도 없는 경우(신규로 비공개 전환 등)는 비밀번호 필수
                throw new GlobalException(ErrorCode.CHALLENGE_PRIVATE_PASSWORD_REQUIRED);
            }
            // 미입력 + 기존 비밀번호 있음 -> 기존 비밀번호를 유지하므로 통과
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
     * 챌린지 수정 기능 - 수정 요청의 비밀번호 필드를 실제 저장할 비밀번호로 변환
     * - 공개 전환 시 비밀번호는 null
     * - 비공개이면서 새 비밀번호가 입력된 경우 그 값으로 교체 (형식은 validateUpdateRequest에서 이미 검증됨)
     * - 비공개이면서 비밀번호 미입력인 경우 기존 비밀번호 유지 (validateUpdateRequest를 통과했다면 기존 비밀번호가 반드시 존재함)
     */
    private String resolveUpdatedPassword(Challenge challenge, ChallengeRequestDto.UpdateChallengeDto req) {
        if (req.getIsPublic()) {
            return null;
        }
        boolean isPasswordProvided = req.getPassword() != null && !req.getPassword().isBlank();
        return isPasswordProvided ? req.getPassword() : challenge.getPassword();
    }

	/**
	 * 챌린지 생성 요청에 대한 비즈니스 검증 로직
	 */
	private void validateCreateRequest(User owner, ChallengeRequestDto.CreateChallengeDto req) {

		// 참가 중인 챌린지가 5개일 경우 요청 거절
		if (challengeRepository.countByUserIdAndStatus(owner.getId(), ChallengeJoinStatus.JOINED) >= 5) {
			throw new GlobalException(ErrorCode.MAX_CHALLENGE_EXCEEDED);
		}

        LocalDate todayKst = LocalDate.now(ZoneId.of("Asia/Seoul"));

		// 시작일 검증: 오늘 이후여야 함
		if (!req.getStartDate().isAfter(LocalDate.now())) {
			throw new GlobalException(ErrorCode.CHALLENGE_INVALID_START_DATE);
		}

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

		// 참가 중인 챌린지가 5개일 경우 요청 거절
		if (challengeRepository.countByUserIdAndStatus(user.getId(), ChallengeJoinStatus.JOINED) >= 5) {
			throw new GlobalException(ErrorCode.MAX_CHALLENGE_EXCEEDED);
		}

		// 상태 검증 (종료만 아니면 모두 참여 가능)
		if (challenge.getStatus() == ChallengeStatus.FINISHED) {
			throw new GlobalException(ErrorCode.CHALLENGE_NOT_RECRUITING);
		}

		// 중복 참가 검증
		userChallengeRepository.findByUserAndChallenge(user, challenge).ifPresent(uc -> {
			if (uc.getStatus() == ChallengeJoinStatus.JOINED) {
				throw new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED);
			}
			if (uc.getStatus() == ChallengeJoinStatus.KICKED) {
				throw new GlobalException(ErrorCode.CHALLENGE_KICKED_USER);
			}
		});

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

		// 라운드가 존재하지 않는 경우
		if (challenge.getCurrentRound() == null) {

			log.error("[Data Error] 챌린지의 현재 라운드(CurrentRound)가 null입니다. challengeId={}", challenge.getId());
			throw new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR);
		}
	}

	// 챌린지 일반 조회용
	private Challenge findChallenge(Long challengeId) {
		return challengeRepository.findById(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
	}

	/**
	 * 챌린지 참가 시 참가자 수 정합성 보장을 위한 락 조회
	 */
	private Challenge findChallengeForUpdate(Long challengeId) {
		return challengeRepository.findByIdForUpdate(challengeId)
				.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
	}

    // 상단 정보 조회용 (요일 정보 Fetch Join)
    private Challenge findChallengeWithDays(Long challengeId) {
        return challengeRepository.findByIdWithDays(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
    }

    /**
     * 챌린지 종료일까지 남은 일수 계산
     * - 챌린지 하단 버튼에 표시 목적
     */
	private long calculateRemainDays(LocalDate endDate) {
		if (endDate == null) return 0;
		long days = ChronoUnit.DAYS.between(LocalDate.now(), endDate);
		return Math.max(days, 0); // 과거 날짜라면 0 반환
	}

    /**
     * 유저가 '오늘' '해당 챌린지의 인증 시간' 내에 '완료된 인증'을 했는지 확인
     * (추후 기획에 따라 시간대 상관없이 하루 전체 조회로 변경 가능)
     */
    private boolean checkTodayVerification(Long userChallengeId, Challenge challenge) {
        LocalDate today = LocalDate.now();

        // 챌린지 인증 허용 시간대
        LocalTime verifyStart = challenge.getVerifyStartTime();
        LocalTime verifyEnd = challenge.getVerifyEndTime();

        LocalDateTime start = LocalDateTime.of(today, verifyStart);
        LocalDateTime end = LocalDateTime.of(today, verifyEnd);

        return verificationRepository.existsTodayVerification(
                userChallengeId,
                VerificationStatus.COMPLETED,
                start,
                end
        );
    }

    /**
     * 하단 버튼의 상태(ActionButtonStatus)를 결정하는 핵심 로직
     * (기획 따라 변경 가능)
     */
	private ActionButtonStatus resolveButtonStatus(Challenge challenge, boolean isParticipant, boolean isCertifiedToday, boolean isMaxJoined, boolean isKicked) {

		// 1. 챌린지 자체가 종료된 경우
		if (challenge.getStatus() == ChallengeStatus.FINISHED) {
			return ActionButtonStatus.FINISHED;
		}

		// 2. 강퇴된 유저의 경우
		if (isKicked) {
			return ActionButtonStatus.REJECT;
		}

		// 3. 참여자인 경우 (인증 관련 분기)
		if (isParticipant) {
			Round currentRound = challenge.getCurrentRound();

			if (currentRound != null && LocalDate.now().isBefore(currentRound.getStartDate())) {
				return ActionButtonStatus.UPCOMING;
			}

			if (!isTodayVerificationDay(challenge)) {
				return ActionButtonStatus.NOT_DAY;
			}

			if (!isNowWithinVerificationTime(challenge)) {
				return ActionButtonStatus.NOT_TIME;
			}

			return isCertifiedToday ? ActionButtonStatus.DONE : ActionButtonStatus.AVAILABLE;
		}

		// 4. 미참여자이면서 참여가 제한되는 경우
		if (isMaxJoined) {
			return ActionButtonStatus.MAX_LIMIT_EXCEEDED;
		}

		// 5. 미참여자 정원 체크
		if (challenge.getCurrentParticipants() >= challenge.getMaxParticipants()) {
			return ActionButtonStatus.WAITLIST;
		}

		// 6. 그 외 참여 가능
		return ActionButtonStatus.JOIN;
	}

    /**
     * 오늘이 챌린지 인증 요일(DaysOfWeek)에 포함되는지 확인
     */
    private boolean isTodayVerificationDay(Challenge challenge) {
        List<ChallengeDayJoin> challengeDays = challenge.getChallengeDays();
        ChallengeDays todayChallengeDay = getTodayChallengeDay();

        return challengeDays.stream()
                .anyMatch(join -> join.getDayOfWeek() == todayChallengeDay);
    }

    /**
     * 현재 시간이 챌린지 인증 시간대(Start ~ End)에 포함되는지 확인
     */
    private boolean isNowWithinVerificationTime(Challenge challenge) {
        LocalTime now = LocalTime.now();

        LocalTime start = challenge.getVerifyStartTime();
        LocalTime end   = challenge.getVerifyEndTime();

        return !now.isBefore(start) && !now.isAfter(end);
    }

    /**
     * Java DayOfWeek -> Custom ChallengeDays Enum 변환
     */
    private ChallengeDays getTodayChallengeDay() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        return switch (dayOfWeek) {
            case MONDAY -> ChallengeDays.MONDAY;
            case TUESDAY -> ChallengeDays.TUESDAY;
            case WEDNESDAY -> ChallengeDays.WEDNESDAY;
            case THURSDAY -> ChallengeDays.THURSDAY;
            case FRIDAY -> ChallengeDays.FRIDAY;
            case SATURDAY -> ChallengeDays.SATURDAY;
            case SUNDAY -> ChallengeDays.SUNDAY;
        };
    }

}
