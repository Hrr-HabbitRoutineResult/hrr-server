package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiscordAlertThrottleTest {

    @Test
    void tryReserve_suppressesSameKey_whileDeliveryIsInProgress() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.tryReserve("key", now)).isTrue();
        assertThat(throttle.tryReserve("key", now + 1_000L)).isFalse();
    }

    @Test
    void markDelivered_suppressesSameKey_withinDedupWindow() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.tryReserve("key", now)).isTrue();
        throttle.markDelivered("key", now + 100L);

        assertThat(throttle.tryReserve("key", now + 1_000L)).isFalse();
    }

    @Test
    void release_allowsSameKeyToRetryImmediately_afterDeliveryFailure() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.tryReserve("key", now)).isTrue();
        throttle.release("key");

        assertThat(throttle.tryReserve("key", now + 1L)).isTrue();
    }

    @Test
    void tryReserve_appliesDedupWindowBoundaryInclusively() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.tryReserve("key", now)).isTrue();
        throttle.markDelivered("key", now);

        assertThat(throttle.tryReserve("key", now + 299_999L)).isFalse();
        assertThat(throttle.tryReserve("key", now + 300_000L)).isTrue();
    }

    @Test
    void tryReserve_treatsDifferentKeysIndependently() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.tryReserve("key-a", now)).isTrue();
        assertThat(throttle.tryReserve("key-b", now)).isTrue();
    }

    @Test
    void tryReserve_removesExpiredDeliveredKeys_duringPeriodicCleanup() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        throttle.tryReserve("expired-a", now);
        throttle.markDelivered("expired-a", now);
        throttle.tryReserve("expired-b", now);
        throttle.markDelivered("expired-b", now);
        assertThat(throttle.trackedKeyCount()).isEqualTo(2);

        throttle.tryReserve("current", now + 300_001L);

        assertThat(throttle.trackedKeyCount()).isEqualTo(1);
        assertThat(throttle.tryReserve("current", now + 300_002L)).isFalse();
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
