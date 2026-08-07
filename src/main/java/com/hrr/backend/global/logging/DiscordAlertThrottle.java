package com.hrr.backend.global.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discord 알림 폭탄 방지용 경량 스로틀.
 * (동일 원인 반복 억제 + 분당 전송 상한)
 */
public class DiscordAlertThrottle {

    private final long dedupWindowMillis;
    private final int maxPerMinute;

    private final ConcurrentHashMap<String, Long> lastSentAt = new ConcurrentHashMap<>();

    private final AtomicLong windowStart = new AtomicLong(0L);
    private final AtomicInteger windowCount = new AtomicInteger(0);

    public DiscordAlertThrottle(int dedupWindowSeconds, int maxPerMinute) {
        this.dedupWindowMillis = dedupWindowSeconds * 1000L;
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * key가 dedupWindow 내에 이미 전송된 적이 있으면 true(억제 대상).
     * 아니면 전송 시각을 기록하고 false를 반환한다.
     */
    public boolean shouldSuppress(String key, long nowMillis) {
        Long last = lastSentAt.get(key);
        if (last != null && (nowMillis - last) < dedupWindowMillis) {
            return true;
        }
        lastSentAt.put(key, nowMillis);
        return false;
    }

    /**
     * 60초 고정 윈도우 내 최대 maxPerMinute건까지만 전송을 허용한다.
     */
    public boolean tryAcquireGlobalSlot(long nowMillis) {
        long currentWindowStart = windowStart.get();

        if (nowMillis - currentWindowStart > 60_000L) {
            if (windowStart.compareAndSet(currentWindowStart, nowMillis)) {
                windowCount.set(0);
            }
        }

        return windowCount.incrementAndGet() <= maxPerMinute;
    }
}
