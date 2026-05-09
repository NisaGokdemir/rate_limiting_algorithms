package org.kafka.rate_limiting;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Sliding Window Log (Kayan Pencere Günlüğü) Algoritması
 * Hız sınırlama işlemlerinde en yüksek hassasiyeti sağlar.
 */
public class SlidingWindowLog {
    private final long windowSizeInSeconds;   // Pencere boyutu (saniye) | Size of the sliding window in seconds
    private final long maxRequestsPerWindow;  // Pencere başına maksimum istek | Maximum number of requests allowed
    private final Queue<Long> requestLog;     // İstek zaman damgalarının tutulduğu günlük | Log of request timestamps

    public SlidingWindowLog(long windowSizeInSeconds, long maxRequestsPerWindow) {
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.requestLog = new LinkedList<>();
    }

    /**
     * İsteğe izin verilip verilmediğini, geçmiş kayıtları tek tek kontrol ederek belirler.
     * Checks if the request is allowed by verifying exact timestamps in the log.
     */
    public synchronized boolean allowRequest() {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSizeInSeconds; // Mevcut pencerenin başlangıç sınırı

        // Mevcut pencerenin dışında kalan (eskimiş) tüm kayıtları temizle
        // Remove timestamps that are outside of the current window
        while (!requestLog.isEmpty() && requestLog.peek() <= windowStart) {
            requestLog.poll();
        }

        // Günlükteki kayıt sayısı limitten az ise yeni isteğe izin ver
        if (requestLog.size() < maxRequestsPerWindow) {
            requestLog.offer(now);  // Bu isteği günlüğe kaydet | Log this request
            return true;            // İsteğe izin ver | Allow the request
        }

        return false;  // Limit aşıldı, isteği reddet | Limit exceeded, deny the request
    }
}