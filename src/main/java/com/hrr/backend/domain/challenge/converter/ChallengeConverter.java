package com.hrr.backend.domain.challenge.converter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.entity.enums.ExtensionStatus;
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
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;

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
            boolean isOwnerActive,
            LocalDate startDate,
            LocalDate endDate,
            long remainDays,
            boolean isParticipant,
            boolean isLiked,
            ActionButtonStatus actionButtonStatus,
            boolean isOwner
    ) {
        // 방장 정보 마스킹: owner가 null(정보 삭제됨)이거나 비활성 상태(탈퇴 처리됨)인 경우 통합 처리
        String nickname;
        String profileImageUrl = null;
        Long ownerId = null;

        if (owner != null && isOwnerActive) {
            nickname = owner.getDisplayNickname();
            profileImageUrl = owner.getProfileImage();
            ownerId = owner.getId();
        } else {
            // 그 외 모든 경우 "탈퇴한 회원"으로 표시
            nickname = "탈퇴한 사용자";
        }

        ChallengeResponseDto.OwnerDto ownerDto = ChallengeResponseDto.OwnerDto.builder()
                .id(ownerId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();

        return ChallengeResponseDto.HeaderInfoDto.builder()
                .challengeId(challenge.getId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .verificationType(challenge.getVerificationType())
                .imageUrl(s3UrlUtil.toFullUrl(challenge.getImageKey()))
                .currentParticipantCount(challenge.getCurrentParticipants())
                .maxParticipantCount(challenge.getMaxParticipants())
                .startDate(startDate)
                .endDate(endDate)
                .remainDays(remainDays)
                .isPublic(challenge.getIsPublic())
                .isObserverMode(challenge.getIsViewerMode())
                .isParticipant(isParticipant)
                .isOwner(isOwner)
                .isLiked(isLiked)
                .owner(ownerDto)
                .actionButtonStatus(actionButtonStatus)
                .build();
    }

    // 챌린지 프로필 DTO
    public ChallengeResponseDto.ChallengeProfileDto toProfileDto(
            Challenge challenge,
            boolean isParticipating,
            List<ChallengeDays> verifiedDays,
            ExtensionStatus extensionStatus
    ) {
        // Entity의 ChallengeDayJoin 리스트를 Enum 리스트로 변환
        List<ChallengeDays> targetDays = challenge.getChallengeDays().stream()
                .map(ChallengeDayJoin::getDay)
                .sorted()
                .toList();

        List<ChallengeDays> sortedVerifiedDays = null;
        if (verifiedDays != null) {
            sortedVerifiedDays = verifiedDays.stream()
                    .sorted()
                    .toList();
        }

        return ChallengeResponseDto.ChallengeProfileDto.builder()
                .challengeId(challenge.getId())
                .isParticipating(isParticipating)
                .rule(challenge.getRule())
                .targetDays(targetDays)
                .verifyStartTime(challenge.getVerifyStartTime())
                .verifyEndTime(challenge.getVerifyEndTime())
                .verifiedDaysThisWeek(sortedVerifiedDays)// 미참여시 null
                .extensionStatus(extensionStatus)
                .build();
    }

    // Round 엔티티 -> RoundDto 변환
    public ChallengeResponseDto.RoundDto toRoundDto(Round round, boolean isCurrentRound, boolean isParticipated) {
        return ChallengeResponseDto.RoundDto.builder()
                .roundNumber(round.getRoundNumber())
                .isCurrentRound(isCurrentRound)
                .isParticipated(isParticipated)
                .build();
    }

    public ChallengeResponseDto.JoinChallengeDto toJoinResponseDto(Challenge challenge) {
        return new ChallengeResponseDto.JoinChallengeDto(
                challenge.getId(),
                challenge.getCurrentParticipants()
        );
    }

    public ChallengeResponseDto.UpdateChallengeDto toUpdateResponseDto(Challenge challenge) {
        return new ChallengeResponseDto.UpdateChallengeDto(challenge.getId());
    }

    public ChallengeResponseDto.LeaveChallengeDto toLeaveResponseDto(Challenge challenge) {
        return new ChallengeResponseDto.LeaveChallengeDto(
                challenge.getId(),
                challenge.getCurrentParticipants()
        );
    }

    public ChallengeResponseDto.EditInfoDto toEditInfoDto(Challenge challenge) {
        // Entity의 ChallengeDayJoin 리스트를 Enum 리스트로 변환 (toProfileDto와 동일한 방식)
        List<ChallengeDays> daysOfWeek = challenge.getChallengeDays().stream()
                .map(ChallengeDayJoin::getDay)
                .sorted()
                .toList();

        return ChallengeResponseDto.EditInfoDto.builder()
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .isPublic(challenge.getIsPublic())
                .hasPassword(challenge.getPassword() != null && !challenge.getPassword().isBlank())
                .category(challenge.getCategory())
                .verificationType(challenge.getVerificationType())
                .startDate(challenge.getStartDate().toLocalDate())
                .maxParticipants(challenge.getMaxParticipants())
                .isViewerMode(challenge.getIsViewerMode())
                .rule(challenge.getRule())
                .verifyStartTime(challenge.getVerifyStartTime())
                .verifyEndTime(challenge.getVerifyEndTime())
                .daysOfWeek(daysOfWeek)
                .imageKey(challenge.getImageKey())
                .imageUrl(s3UrlUtil.toFullUrl(challenge.getImageKey()))
                .build();
    }

    // 챌린지 참가 중인 챌린저 정보 DTO 변환
    public ChallengeResponseDto.ParticipantDto toParticipantDto(
            UserChallenge userChallenge,
            Long currentUserId,
            boolean isFollowing
    ) {
        User participant = userChallenge.getUser();

        // 본인 여부 (null-safe 비교)
        boolean isMe = Objects.equals(participant.getId(), currentUserId);

        return ChallengeResponseDto.ParticipantDto.builder()
                .userId(participant.getId())
                .nickname(participant.getDisplayNickname())
                .profileImageUrl(s3UrlUtil.toFullUrl(participant.getProfileImage()))
                .isOwner(userChallenge.getRole() == UserChallengeRole.OWNER)
                .isMe(isMe)
                // 본인 프로필에는 팔로우 버튼이 노출되지 않으므로 항상 false로 내려줌
                .isFollowing(isMe ? false : isFollowing)
                .build();
    }
}
