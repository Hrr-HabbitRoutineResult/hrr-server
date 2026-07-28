package com.hrr.backend.domain.challenge.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

class ChallengeTest {

	@Test
	@DisplayName("참가자 수가 0이면 감소할 수 없다")
	void decreaseCurrentParticipants_ThrowsException_WhenCurrentParticipantsIsZero() {
		Challenge challenge = Challenge.builder()
				.currentParticipants(0)
				.build();

		assertThatThrownBy(challenge::decreaseCurrentParticipants)
				.isInstanceOf(GlobalException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHALLENGE_PARTICIPANT_COUNT_UNDERFLOW);
	}
}
