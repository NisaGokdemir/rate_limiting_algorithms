package org.kafka.rate_limiting;

import java.time.Instant;

/**
 * Fixed Window Counter (Sabit Pencere Sayacı) Algoritması
 * Belirli zaman aralıklarında (pencerelerde) istek sayısını sınırlar.
 */
public class FixedWindowCounter {
    private final long windowSizeInSeconds;  // Her bir pencerenin saniye cinsinden boyutu | Size of each window in seconds
    private final long maxRequestsPerWindow; // Her pencerede izin verilen maksimum istek sayısı | Maximum number of requests allowed per window
    private long currentWindowStart;         // Mevcut pencerenin başlangıç zamanı | Start time of the current window
    private long requestCount;               // Mevcut pencere içindeki istek sayısı | Number of requests in the current window

    public FixedWindowCounter(long windowSizeInSeconds, long maxRequestsPerWindow) {
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.currentWindowStart = Instant.now().getEpochSecond();
        this.requestCount = 0;
    }

    /**
     * İsteğe izin verilip verilmediğini kontrol eder.
     * Checks if the request is allowed within the current fixed window.
     */
    public synchronized boolean allowRequest() {
        long now = Instant.now().getEpochSecond();

        // Yeni bir pencereye geçilip geçilmediğini kontrol et
        // Check if we've moved to a new window
        if (now - currentWindowStart >= windowSizeInSeconds) {
            currentWindowStart = now;  // Yeni pencereyi başlat | Start a new window
            requestCount = 0;          // Yeni pencere için sayacı sıfırla | Reset the count for the new window
        }

        // Mevcut pencere limiti aşılmadıysa izin ver
        if (requestCount < maxRequestsPerWindow) {
            requestCount++;  // Sayacı artır | Increment the count for this window
            return true;     // İsteğe izin ver | Allow the request
        }

        return false; // Limit aşıldı, isteği reddet | We've exceeded the limit, deny the request
    }
}
