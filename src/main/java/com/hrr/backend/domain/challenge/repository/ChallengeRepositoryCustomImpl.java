package com.hrr.backend.domain.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.QChallenge;
import com.hrr.backend.domain.challenge.entity.QChallengeDayJoin;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ChallengeRepositoryCustomImpl implements ChallengeRepositoryCustom{

	private final JPAQueryFactory jpaQueryFactory;
	private final QChallenge qChallenge = QChallenge.challenge;
	private final QChallengeDayJoin qChallengeDayJoin = QChallengeDayJoin.challengeDayJoin;

	@Override
	public Slice<ChallengeResponseDto.InfoDto> findChallengesWithFilters(
		Category category,
		LocalDateTime upcomingStartDate,
		LocalDateTime upcomingEndDate,
		SortType sortType,
		List<ChallengeDays> days,
		String title,
		Pageable pageable) {

		List<ChallengeResponseDto.InfoDto> content = jpaQueryFactory
			// Projections.fields()를 사용하여 엔티티가 아닌 InfoDto로 바로 매핑
			.select(Projections.fields(ChallengeResponseDto.InfoDto.class,
				qChallenge.id.as("challengeId"),
				qChallenge.title,
				qChallenge.description,
				qChallenge.currentParticipants.as("currentParticipantCount"),
				qChallenge.maxParticipants.as("maxParticipantCount"),
				qChallenge.imageUrl.as("thumbnailUrl"),
				qChallenge.likeCount,
				qChallenge.startDate
			))
			.from(qChallenge)
			.leftJoin(qChallenge.challengeDays, qChallengeDayJoin)
			.where(
				categoryEq(category),
				startDateBetween(upcomingStartDate, upcomingEndDate),
				titleContains(title),
				dayIn(days)
			)
			.distinct()
			.orderBy(getOrderSpecifier(sortType))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize() + 1)
			.fetch();

		// Slice 객체 생성 및 반환
		boolean hasNext = content.size() > pageable.getPageSize();
		if (hasNext) {
			content.remove(pageable.getPageSize());	// 다음꺼 있는지 판단을 위한 '요청한 개수 다음꺼 데이터' 제거
		}

		return new SliceImpl<>(content, pageable, hasNext);
	}


	//-------동적 쿼리 관련 메소드들------------
	// 필터링 및 정렬 조건이 없을 경우, null을 반환하여 WHERE 절에서 자동 제외 처리

	private BooleanExpression categoryEq(Category category) {
		// category가 null이거나 ALL이면 null을 반환하여 WHERE 절에서 제외
		if (category == null || category == Category.ALL) {
			return null;
		}

		return qChallenge.category.eq(category);
	}

	private BooleanExpression startDateBetween(LocalDateTime upcomingStartDate, LocalDateTime upcomingEndDate) {
		if (upcomingStartDate == null || upcomingEndDate == null) {
			// 조회 쿼리에서 isUpcoming=false인 경우; 즉 필터 적용 x
			return null;
		}

		return qChallenge.startDate.between(upcomingStartDate, upcomingEndDate);
	}

	private BooleanExpression titleContains(String title) {
		return StringUtils.hasText(title) ? qChallenge.title.contains(title) : null;
	}

	private OrderSpecifier<?> getOrderSpecifier(SortType sortType) {
		// 정렬 필터 없을 때 기본 인기순 적용
		SortType finalSortType = (sortType != null) ? sortType : SortType.POPULAR;

		return switch (finalSortType) {
			case POPULAR -> qChallenge.likeCount.desc();

			case OLDEST -> qChallenge.createdAt.asc();

			case LATEST -> qChallenge.createdAt.desc();
		};
	}

	private BooleanExpression dayIn(List<ChallengeDays> days) {
		if (days == null || days.isEmpty()) {
			return null;
		}

		return qChallengeDayJoin.dayOfWeek.in(days);
	}
}
