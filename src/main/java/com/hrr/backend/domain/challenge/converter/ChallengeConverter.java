package com.hrr.backend.domain.challenge.converter;

import java.time.LocalDate;
import java.util.List;

import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.global.s3.S3UrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;

@Component
@RequiredArgsConstructor
public class ChallengeConverter {

    private final S3UrlUtil s3UrlUtil;

    public Challenge toChallengeEntity(
            ChallengeRequestDto.CreateChallengeDto req,
            boolean isPublic,
            boolean isViewerMode,
            String password
    ) {
        Challenge challenge = Challenge.builder()
                .isPublic(isPublic)
                .category(req.getCategory())
                .isViewerMode(isViewerMode)
                .maxParticipants(req.getMaxParticipants())
                .password(password)
                .title(req.getTitle())
                .description(req.getDescription())
                .startDate(req.getStartDate().atStartOfDay())
                .verificationType(req.getVerificationType())
                .verifyStartTime(req.getVerifyStartTime())
                .verifyEndTime(req.getVerifyEndTime())
                .rule(req.getRule())
                .currentParticipants(1)
                .status(ChallengeStatus.UPCOMING)
                .imageKey(req.getImageKey())
                .likeCount(0)
                .build();

        List<ChallengeDays> daysOfWeek = req.getDaysOfWeek();
        for (ChallengeDays day : daysOfWeek) {
            ChallengeDayJoin join = ChallengeDayJoin.builder()
                    .challenge(challenge)
                    .dayOfWeek(day)
                    .build();
            challenge.getChallengeDays().add(join);
        }

        return challenge;
    }

    public ChallengeResponseDto.CreateChallengeDto toCreateResponseDto(Challenge challenge) {
        return new ChallengeResponseDto.CreateChallengeDto(challenge.getId());
    }

    // 좋아요 응답 DTO 변환 메서드
    public ChallengeResponseDto.ChallengeLikeDto toChallengeLikeDto(Challenge challenge, Boolean isLiked) {
        return ChallengeResponseDto.ChallengeLikeDto.builder()
                .challengeId(challenge.getId())
                .isLiked(isLiked)
                .likeCount(challenge.getLikeCount())
                .build();
    }

    public ChallengeResponseDto.HeaderInfoDto toHeaderInfoDto(
            Challenge challenge,
            User owner,
            LocalDate startDate,
            LocalDate endDate,
            long remainDays,
            boolean isParticipant,
            boolean isLiked,
            ActionButtonStatus actionButtonStatus
    ) {
        // 방장 정보 DTO 생성
        ChallengeResponseDto.OwnerDto ownerDto = ChallengeResponseDto.OwnerDto.builder()
                .id(owner.getId())
                .nickname(owner.getNickname())
                .profileImageUrl(owner.getProfileImage())
                .build();

        // 상단 정보 DTO 생성 및 반환
        return ChallengeResponseDto.HeaderInfoDto.builder()
                .challengeId(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .imageUrl(s3UrlUtil.toFullUrl(challenge.getImageKey()))
                .currentParticipantCount(challenge.getCurrentParticipants())
                .maxParticipantCount(challenge.getMaxParticipants())
                .startDate(startDate)
                .endDate(endDate)
                .remainDays(remainDays)
				.isPublic(challenge.getIsPublic())
                .isObserverMode(challenge.getIsViewerMode())
                .isParticipant(isParticipant)
                .isLiked(isLiked)
                .owner(ownerDto)
                .actionButtonStatus(actionButtonStatus)
                .build();
    }

    // 챌린지 프로필 DTO
    public ChallengeResponseDto.ChallengeProfileDto toProfileDto(
            Challenge challenge,
            boolean isParticipating,
            List<ChallengeDays> verifiedDays
    ) {
        // Entity의 ChallengeDayJoin 리스트를 Enum 리스트로 변환
        List<ChallengeDays> targetDays = challenge.getChallengeDays().stream()
                .map(ChallengeDayJoin::getDay)
                .toList();

        return ChallengeResponseDto.ChallengeProfileDto.builder()
                .challengeId(challenge.getId())
                .isParticipating(isParticipating)
                .rule(challenge.getRule())
                .targetDays(targetDays)
                .verifyStartTime(challenge.getVerifyStartTime())
                .verifyEndTime(challenge.getVerifyEndTime())
                .verifiedDaysThisWeek(verifiedDays) // 미참여시 null
                .build();
    }

    // Round 엔티티 -> RoundDto 변환
    public ChallengeResponseDto.RoundDto toRoundDto(Round round, boolean isCurrentRound) {
        return ChallengeResponseDto.RoundDto.builder()
                .roundNumber(round.getRoundNumber())
                .isCurrentRound(isCurrentRound)
                .build();
    }
}
