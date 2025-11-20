package com.hrr.backend.domain.user.converter;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import org.springframework.stereotype.Component;

@Component
public class UserChallengeConverter {

    /**
     * Create a UserChallenge that associates the given user with the given challenge as the owner.
     *
     * @param user      the user to assign the OWNER role
     * @param challenge the challenge to which the user will be assigned
     * @return          the created UserChallenge with its role set to `OWNER`
     */
    public UserChallenge toOwner(User user, Challenge challenge) {
        return UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.OWNER)
                .build();
    }

    /**
     * Creates a UserChallenge that associates the given user with the given challenge and assigns the CHALLENGER role.
     *
     * @param user the user to associate with the challenge
     * @param challenge the challenge to associate with the user
     * @return a UserChallenge instance with role set to `UserChallengeRole.CHALLENGER`
     */
    public UserChallenge toChallenger(User user, Challenge challenge) {
        return UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.CHALLENGER)
                .build();
    }
}