package com.hrr.backend.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class TestControllerTest {

    private final TestController controller = new TestController(mock(TestService.class));

    @Test
    void maskIfSensitive_masksUntrustedAppleErrorValue() {
        assertThat(controller.maskIfSensitive("error", "access_token=secret"))
                .isEqualTo("***MASKED***");
    }

    @Test
    void maskIfSensitive_keepsExplicitlyAllowedDiagnosticValue() {
        assertThat(controller.maskIfSensitive("Content-Type", "application/x-www-form-urlencoded"))
                .isEqualTo("application/x-www-form-urlencoded");
    }
}
