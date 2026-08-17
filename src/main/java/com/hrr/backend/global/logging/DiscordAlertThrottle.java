package com.hrr.backend.global.logging;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Discord 알림 폭탄 방지용 경량 스로틀.
 * (동일 원인 반복 억제 + 분당 전송 상한)
 */
public class DiscordAlertThrottle {

    private enum DeliveryState {
        RESERVED,
        DELIVERED
    }

    private record DedupEntry(DeliveryState state, long timestamp) {
    }

    private final long dedupWindowMillis;
    private final int maxPerMinute;

    private final ConcurrentHashMap<String, DedupEntry> dedupEntries = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupAt = new AtomicLong(0L);

    private long windowStart;
    private int windowCount;

    public DiscordAlertThrottle(int dedupWindowSeconds, int maxPerMinute) {
        this.dedupWindowMillis = dedupWindowSeconds * 1000L;
        this.maxPerMinute = maxPerMinute;
    }

    /**
     * 동일 key가 전송 중이거나 dedupWindow 내에 전송 성공했다면 false를 반환한다.
     * 전송을 시도해도 된다면 RESERVED 상태로 원자적으로 예약하고 true를 반환한다.
     */
    public boolean tryReserve(String key, long nowMillis) {
        cleanupExpiredEntries(nowMillis);

        boolean[] reserved = {false};
        dedupEntries.compute(key, (ignored, current) -> {
            if (current == null
                    || (current.state() == DeliveryState.DELIVERED
                    && nowMillis - current.timestamp() >= dedupWindowMillis)) {
                reserved[0] = true;
                return new DedupEntry(DeliveryState.RESERVED, nowMillis);
            }
            return current;
        });
        return reserved[0];
    }

    /** Discord가 2xx로 응답한 시점부터 dedupWindow를 적용한다. */
    public void markDelivered(String key, long deliveredAtMillis) {
        dedupEntries.computeIfPresent(key, (ignored, current) ->
                current.state() == DeliveryState.RESERVED
                        ? new DedupEntry(DeliveryState.DELIVERED, deliveredAtMillis)
                        : current);
    }

    /**
     * rate limit, queue 포화, 최종 전송 실패 시 예약을 해제한다.
     * 따라서 다음 동일 오류가 5분을 기다리지 않고 다시 전송을 시도할 수 있다.
     */
    public void release(String key) {
        dedupEntries.computeIfPresent(key, (ignored, current) ->
                current.state() == DeliveryState.RESERVED ? null : current);
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

        dedupEntries.entrySet().removeIf(entry ->
                entry.getValue().state() == DeliveryState.DELIVERED
                        && nowMillis - entry.getValue().timestamp() >= dedupWindowMillis);
    }

    int trackedKeyCount() {
        return dedupEntries.size();
    }
}
