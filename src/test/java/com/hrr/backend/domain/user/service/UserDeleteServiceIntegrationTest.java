package com.hrr.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrr.backend.domain.auth.entity.SocialAuth;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.auth.repository.SocialAuthRepository;
import com.hrr.backend.domain.auth.service.AuthService;
import com.hrr.backend.domain.follow.service.FollowCountService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.s3.S3Service;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserDeleteServiceIntegrationTest {

	@Autowired private UserDeleteService userDeleteService;
	@Autowired private UserRepository userRepository;
	@Autowired private SocialAuthRepository socialAuthRepository;
	@Autowired private TransactionTemplate transactionTemplate;

	@MockitoBean private AuthService authService;
	@MockitoBean private S3Service s3Service;
	@MockitoBean private FollowCountService followCountService;

	@AfterEach
	void tearDown() {
		socialAuthRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void processPermanentWithdrawal_DeletesSocialAuthWithoutMergingStaleEntity() {
		Long userId = transactionTemplate.execute(status -> {
			User user = userRepository.save(User.builder()
					.name("withdrawal target")
					.nickname("withdrawal-target")
					.email("withdrawal@example.com")
					.deletedAt(LocalDateTime.now().minusMonths(2))
					.userStatus(UserStatus.INACTIVE)
					.build());

			socialAuthRepository.save(SocialAuth.builder()
					.user(user)
					.socialType(SocialType.KAKAO)
					.socialId("social-id")
					.build());

			return user.getId();
		});

		assertThatCode(() -> userDeleteService.processPermanentWithdrawal(userId))
				.doesNotThrowAnyException();

		User deletedUser = userRepository.findById(userId).orElseThrow();
		assertThat(deletedUser.getUserStatus()).isEqualTo(UserStatus.DELETED);
		assertThat(deletedUser.getEmail()).isNull();
		assertThat(socialAuthRepository.findByUser(deletedUser)).isEmpty();
	}
}
