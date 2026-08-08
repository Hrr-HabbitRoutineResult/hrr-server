package com.hrr.backend.global.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discord 알림 폭탄 방지용 경량 스로틀.
 * (동일 원인 반복 억제 + 분당 전송 상한)
 */
public class DiscordAlertThrottle {

    private final long dedupWindowMillis;
    private final int maxPerMinute;

    private final ConcurrentHashMap<String, Long> lastSentAt = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong(0L);

    private long windowStart;
    private int windowCount;

    public DiscordAlertThrottle(int dedupWindowSeconds, int maxPerMinute) {
        this.dedupWindowMillis = dedupWindowSeconds * 1000L;
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * key가 dedupWindow 내에 이미 전송된 적이 있으면 true(억제 대상).
     * 아니면 전송 시각을 기록하고 false를 반환한다.
     */
    public boolean shouldSuppress(String key, long nowMillis) {
        cleanupExpiredEntries(nowMillis);

        boolean[] suppressed = {false};
        lastSentAt.compute(key, (ignored, last) -> {
            if (last != null && (nowMillis - last) < dedupWindowMillis) {
                suppressed[0] = true;
                return last;
            }
            return nowMillis;
        });
        return suppressed[0];
    }

    /**
     * 60초 고정 윈도우 내 최대 maxPerMinute건까지만 전송을 허용한다.
     */
    public synchronized boolean tryAcquireGlobalSlot(long nowMillis) {
        if (nowMillis - windowStart >= 60_000L) {
            windowStart = nowMillis;
            windowCount = 0;
        }

        windowCount++;
        return windowCount <= maxPerMinute;
    }

    private void cleanupExpiredEntries(long nowMillis) {
        long cleanupIntervalMillis = Math.max(1_000L, Math.min(dedupWindowMillis, 60_000L));
        long previousCleanupAt = lastCleanupAt.get();
        if (nowMillis - previousCleanupAt < cleanupIntervalMillis
                || !lastCleanupAt.compareAndSet(previousCleanupAt, nowMillis)) {
            return;
        }

        lastSentAt.entrySet().removeIf(entry -> nowMillis - entry.getValue() >= dedupWindowMillis);
    }

    int trackedKeyCount() {
        return lastSentAt.size();
    }
}
