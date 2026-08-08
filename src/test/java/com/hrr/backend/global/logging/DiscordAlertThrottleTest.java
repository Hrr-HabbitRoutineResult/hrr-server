package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiscordAlertThrottleTest {

    @Test
    void shouldSuppress_suppressesSameKey_withinDedupWindow() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key", now)).isFalse();
        assertThat(throttle.shouldSuppress("key", now + 1_000L)).isTrue();
    }

    @Test
    void shouldSuppress_allowsAgain_afterDedupWindowPasses() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key", now)).isFalse();
        assertThat(throttle.shouldSuppress("key", now + 300_001L)).isFalse();
    }

    @Test
    void shouldSuppress_treatsDifferentKeysIndependently() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key-a", now)).isFalse();
        assertThat(throttle.shouldSuppress("key-b", now)).isFalse();
    }

    @Test
    void shouldSuppress_removesExpiredKeys_duringPeriodicCleanup() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        throttle.shouldSuppress("expired-a", now);
        throttle.shouldSuppress("expired-b", now);
        assertThat(throttle.trackedKeyCount()).isEqualTo(2);

        throttle.shouldSuppress("current", now + 300_001L);

        assertThat(throttle.trackedKeyCount()).isEqualTo(1);
        assertThat(throttle.shouldSuppress("current", now + 300_002L)).isTrue();
    }

    @Test
    void tryAcquireGlobalSlot_rejectsOnceOverPerMinuteLimit() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 3);
        long now = 1_000_000L;

        assertThat(throttle.tryAcquireGlobalSlot(now)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 10L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 20L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 30L)).isFalse();
    }

    @Test
    void tryAcquireGlobalSlot_resetsCount_afterWindowRollsOver() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 2);
        long now = 1_000_000L;

        assertThat(throttle.tryAcquireGlobalSlot(now)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 10L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 20L)).isFalse();

        assertThat(throttle.tryAcquireGlobalSlot(now + 60_000L)).isTrue();
    }
}
