package com.hrr.backend.domain.user.converter;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import org.springframework.stereotype.Component;

@Component
public class UserChallengeConverter {

    // 챌린지 생성자를 OWNER로 매핑하는 UserChallenge 생성
    public UserChallenge toOwner(User user, Challenge challenge) {
        return UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.OWNER)
                .build();
    }

    // 챌린지 참가자를 CHALLENGER로 매핑하는 UserChallenge 생성
    public UserChallenge toChallenger(User user, Challenge challenge) {
        return UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.CHALLENGER)
                .build();
    }
}