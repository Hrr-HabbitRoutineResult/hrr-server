package com.hrr.backend.domain.user.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.user.dto.UserMissionResponseDto;
import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserMission;
import com.hrr.backend.domain.user.repository.RandomMissionRepository;
import com.hrr.backend.domain.user.repository.UserFavorRepository;
import com.hrr.backend.domain.user.repository.UserMissionRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.s3.S3UrlUtil;
import com.hrr.backend.domain.point.service.PointService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMissionServiceImpl implements UserMissionService {

	private final UserMissionRepository userMissionRepository;
	private final ChallengeRepository challengeRepository;
	private final RandomMissionRepository randomMissionRepository;
	private final UserFavorRepository userFavorRepository;

	private final S3UrlUtil s3UrlUtil;
    private final PointService pointService;

	@Override
	@Transactional
	public UserMissionResponseDto.DetailDto getRandomMission(User user) {
		UserMission userMission;

		//upsert 방식 적용
		LocalDate today = LocalDate.now();

		// 조회 먼저
		Optional<UserMission> optionalUserMission = userMissionRepository.findByUserAndDate(user, today);

		// 이미 미션이 있으면 바로 반환
		if(optionalUserMission.isPresent() && optionalUserMission.get().getMission()!=null) {
			return convertToDetailDto(optionalUserMission.get());
		}

		// 데이터가 없거나 미션이 배정되지 않았으면 생성 시도
		try {
			// 랜덤미션 선택
			RandomMission randomMission = selectRandomMission(user);

			if (optionalUserMission.isEmpty()) {
				// --- 데이터가 없는 경우 ----

				// UserMission 생성
				userMission = UserMission.builder()
					.user(user)
					.mission(randomMission)
					.date(today)
					.isCompleted(false)
					.build();

				// DB에 저장 - 동시성 문제 발생 가능
				userMissionRepository.save(userMission);
			} else {
				// --- 데이터는 있는데, 미션 할당이 안 된 경우 ---
				userMission = optionalUserMission.get();
				userMission.setMission(randomMission);	// 미션만 할당
			}
		} catch (DataIntegrityViolationException e) {
			// 동시성 충돌 발생 시: DB에 이미 저장된 레코드를 재조회하여 복구
			userMission = userMissionRepository.findByUserAndDate(user, today)
				.orElseThrow(() -> {
					log.error("[getRandomMission] 동시성 충돌 후 UserMission 복구에 실패했습니다. userId={}, date={}",
						user.getId(), today, e);

					return new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR);
				});

			// 재조회한 레코드에 미션이 없는 경우 할당
			if (userMission.getMission() == null) {
				userMission.setMission(selectRandomMission(user));
			}
		}

		return convertToDetailDto(userMission);
	}

	@Override
	@Transactional(readOnly = true)
	public Boolean getRandomMissionStatus(User user) {
		LocalDate today = LocalDate.now();

		// UserMission을 조회하고, 존재하면 isCompleted 추출 / 없으면 false
		return userMissionRepository.findByUserAndDate(user, today)
			// 미션 기록이 존재하면 isCompleted 추출
			.map(UserMission::getIsCompleted)
			// 없으면 false 반환
			.orElse(false);
	}

	@Override
	@Transactional
	public void verifyRandomMission(User user, Long missionId, LocalDate date, String imageKey) {
        UserMission userMission = userMissionRepository.findByUserAndDateForUpdate(user, date).orElseThrow(()-> new GlobalException(
                ErrorCode.RANDOM_MISSION_NOT_FOUND)
        );
        // 이미 완료 처리된 미션이면 재인증되어도 포인트를 중복 지급하지 않음
        boolean alreadyCompleted = Boolean.TRUE.equals(userMission.getIsCompleted());
		userMission.setImageKey(imageKey);
		userMission.setIsCompleted(true);

        if (!alreadyCompleted) {
            pointService.earnRandomMissionPoint(user, userMission.getMission());
        }
	}

	// 사용 빈도가 적을 것 같아 별도의 클래스가 아닌 private 메소드로 생성
	private UserMissionResponseDto.DetailDto convertToDetailDto(UserMission userMission) {
		return UserMissionResponseDto.DetailDto.builder()
			.missionId(userMission.getMission().getId())
			.title(userMission.getMission().getTitle())
			.content(userMission.getMission().getContent())
			.isCompleted(userMission.getIsCompleted())
			.imageUrl(s3UrlUtil.toFullUrl(userMission.getMission().getImageKey()))
			.build();
	}

	private RandomMission selectRandomMission(User user) {
		// 참가 중인 챌린지들의 카테고리 추출
		List<Category> categoryList = challengeRepository.findCategoriesByUserId(user.getId());

		// 참가 중인 챌린지가 없을 경우, 온보딩 시 선택한 선호 카테고리 적용
		if (categoryList.isEmpty()) {
			categoryList = userFavorRepository.findCategoriesByUserId(user.getId());
		}

		List<RandomMission> missionList;

		if(categoryList.isEmpty()) {
			// 선호 카테고리 정보가 없을 경우(온보딩 건너뜀 등) - 전체 미션 조회
			missionList = randomMissionRepository.findAll();

		} else {
			// 카테고리 리스트에 해당하는 미션 전체 조회
			missionList = randomMissionRepository.findByCategoryIn(categoryList);
		}

		if (missionList.isEmpty()) {
			throw new GlobalException(ErrorCode.RANDOM_MISSION_NOT_FOUND);
		}

		// 랜덤 인덱스 생성
		int randomIndex = ThreadLocalRandom.current().nextInt(missionList.size());
		return missionList.get(randomIndex);
	}
}
