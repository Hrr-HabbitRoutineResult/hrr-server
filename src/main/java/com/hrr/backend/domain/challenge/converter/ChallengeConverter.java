package com.hrr.backend.domain.challenge.converter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;

@Component
public class ChallengeConverter {

    /**
     * Builds a Challenge entity from a creation request DTO and explicit settings.
     *
     * @param req         the creation DTO containing challenge fields and the list of days of week
     * @param isPublic    whether the challenge is discoverable by others
     * @param isViewerMode whether the challenge is created in viewer-only mode
     * @param password    join password for the challenge, or null if no password is required
     * @param imageUrl    URL of the challenge image, or null if none
     * @return            a Challenge entity populated from the request with ChallengeDayJoin entries for each requested day; defaults currentParticipants to 1, status to UPCOMING, and likeCount to 0
     */
    public Challenge toChallengeEntity(
            ChallengeRequestDto.CreateChallengeDto req,
            boolean isPublic,
            boolean isViewerMode,
            String password,
            String imageUrl
    ) {
        Challenge challenge = Challenge.builder()
                .isPublic(isPublic)
                .category(req.getCategory())
                .isViewerMode(isViewerMode)
                .maxParticipants(req.getMaxParticipants())
                .password(password)
                .title(req.getTitle())
                .description(req.getDescription())
                .startDate(req.getStartDate())
                .verificationType(req.getVerificationType())
                .verifyStartTime(req.getVerifyStartTime())
                .verifyEndTime(req.getVerifyEndTime())
                .rule(req.getRule())
                .currentParticipants(1)
                .status(ChallengeStatus.UPCOMING)
                .imageUrl(imageUrl)
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

    /**
     * Create a response DTO containing the ID of the provided challenge.
     *
     * @param challenge the created Challenge entity
     * @return a CreateChallengeDto containing the challenge ID
     */
    public ChallengeResponseDto.CreateChallengeDto toCreateResponseDto(Challenge challenge) {
        return new ChallengeResponseDto.CreateChallengeDto(challenge.getId());
    }
}