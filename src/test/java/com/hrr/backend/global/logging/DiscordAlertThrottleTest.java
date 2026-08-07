package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiscordAlertThrottleTest {

    @Test
    void 동일_키는_dedup_윈도우_내에서_억제된다() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key", now)).isFalse();
        assertThat(throttle.shouldSuppress("key", now + 1_000L)).isTrue();
    }

    @Test
    void dedup_윈도우가_지나면_다시_허용된다() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key", now)).isFalse();
        assertThat(throttle.shouldSuppress("key", now + 300_001L)).isFalse();
    }

    @Test
    void 서로_다른_키는_독립적으로_판단된다() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 100);
        long now = 1_000_000L;

        assertThat(throttle.shouldSuppress("key-a", now)).isFalse();
        assertThat(throttle.shouldSuppress("key-b", now)).isFalse();
    }

    @Test
    void 분당_상한을_초과하면_거부된다() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 3);
        long now = 1_000_000L;

        assertThat(throttle.tryAcquireGlobalSlot(now)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 10L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 20L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 30L)).isFalse();
    }

    @Test
    void 윈도우가_지나면_상한_카운트가_초기화된다() {
        DiscordAlertThrottle throttle = new DiscordAlertThrottle(300, 2);
        long now = 1_000_000L;

        assertThat(throttle.tryAcquireGlobalSlot(now)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 10L)).isTrue();
        assertThat(throttle.tryAcquireGlobalSlot(now + 20L)).isFalse();

        assertThat(throttle.tryAcquireGlobalSlot(now + 60_001L)).isTrue();
    }
}
