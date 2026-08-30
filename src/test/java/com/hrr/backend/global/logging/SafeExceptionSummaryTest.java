package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeExceptionSummaryTest {

	@Test
	void summarize_IncludesTypesAndApplicationOriginWithoutExceptionMessages() {
		IllegalStateException rootCause = new IllegalStateException("token=secret-token-value");
		rootCause.setStackTrace(new StackTraceElement[] {
				new StackTraceElement(
						"com.hrr.backend.domain.user.service.UserDeleteService",
						"processPermanentWithdrawal",
						"UserDeleteService.java",
						95)
		});
		RuntimeException exception = new RuntimeException("email=user@example.com", rootCause);

		String summary = SafeExceptionSummary.summarize(exception);

		assertThat(summary)
				.contains("type=RuntimeException")
				.contains("rootCause=IllegalStateException")
				.contains("origin=UserDeleteService.processPermanentWithdrawal:95")
				.doesNotContain("secret-token-value", "user@example.com");
	}
}
