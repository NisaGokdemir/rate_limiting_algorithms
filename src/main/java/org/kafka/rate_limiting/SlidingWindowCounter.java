package org.kafka.rate_limiting;

import java.time.Instant;

/**
 * Sliding Window Counter (Kayan Pencere Sayacı) Algoritması
 * Sabit pencerelerin birleşim noktalarındaki aşırı yüklenme sorununu çözer.
 */
public class SlidingWindowCounter {
    private final long windowSizeInSeconds;   // Kayan pencerenin saniye cinsinden boyutu | Size of the sliding window in seconds
    private final long maxRequestsPerWindow;  // Pencere başına izin verilen maksimum istek | Maximum number of requests allowed in the window
    private long currentWindowStart;          // Mevcut pencerenin başlangıç zamanı | Start time of the current window
    private long previousWindowCount;         // Bir önceki penceredeki toplam istek sayısı | Number of requests in the previous window
    private long currentWindowCount;          // Mevcut penceredeki toplam istek sayısı | Number of requests in the current window

    public SlidingWindowCounter(long windowSizeInSeconds, long maxRequestsPerWindow) {
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.currentWindowStart = Instant.now().getEpochSecond();
        this.previousWindowCount = 0;
        this.currentWindowCount = 0;
    }

    /**
     * İsteğe izin verilip verilmediğini ağırlıklı hesaplama ile kontrol eder.
     * Checks if the request is allowed using a weighted counter approach.
     */
    public synchronized boolean allowRequest() {
        long now = Instant.now().getEpochSecond();
        long timePassedInWindow = now - currentWindowStart;

        // Yeni bir pencereye geçilip geçilmediğini kontrol et
        // Check if we've moved to a new window
        if (timePassedInWindow >= windowSizeInSeconds) {
            previousWindowCount = currentWindowCount; // Mevcut olan artık "önceki" oldu
            currentWindowCount = 0;                   // Yeni pencereyi sıfırla
            currentWindowStart = now;                 // Zamanı güncelle
            timePassedInWindow = 0;
        }

        // Ağırlıklı istek sayısını hesapla (Matematiksel Tahmin)
        // Calculate the weighted count of requests
        double overlapWeight = (windowSizeInSeconds - timePassedInWindow) / (double) windowSizeInSeconds;
        double weightedCount = (previousWindowCount * overlapWeight) + currentWindowCount;

        // Eğer ağırlıklı toplam limitin altındaysa izin ver
        if (weightedCount < maxRequestsPerWindow) {
            currentWindowCount++;  // Mevcut pencere sayacını artır | Increment current count
            return true;           // İsteğe izin ver | Allow the request
        }

        return false;  // Limit aşıldı | We've exceeded the limit
    }
}