package org.kafka.rate_limiting;

import java.time.Instant;

/**
 * Token Bucket (Jeton Kovası) Algoritması Uygulaması
 * Hız sınırlama (Rate Limiting) için kullanılır.
 */
public class TokenBucket {
    private final long capacity;        // Kovanın alabileceği maksimum jeton sayısı | Maximum number of tokens the bucket can hold
    private final double fillRate;      // Jeton eklenme hızı (saniyedeki jeton sayısı) | Rate at which tokens are added to the bucket (tokens per second)
    private double tokens;              // Kovadaki güncel jeton sayısı | Current number of tokens in the bucket
    private Instant lastRefillTimestamp; // Kovaya son jeton eklenme zamanı | Last time we refilled the bucket

    public TokenBucket(long capacity, double fillRate) {
        this.capacity = capacity;
        this.fillRate = fillRate;
        this.tokens = capacity;         // Kova dolu olarak başlar | Start with a full bucket
        this.lastRefillTimestamp = Instant.now();
    }

    /**
     * İsteğe izin verilip verilmediğini kontrol eder.
     * Checks if the request is allowed based on token availability.
     */
    public synchronized boolean allowRequest(int tokens) {
        refill();  // Önce geçen süreye göre yeni jetonları ekle | First, add any new tokens based on elapsed time

        // Yeterli jeton yoksa isteği reddet | Not enough tokens, deny the request
        if (this.tokens < tokens) {
            return false;
        }

        this.tokens -= tokens;  // Jetonları tüket | Consume the tokens
        return true;            // İsteğe izin ver | Allow the request
    }

    /**
     * Geçen süreyi hesaplayarak kovayı yeni jetonlarla doldurur.
     * Calculates elapsed time and adds new tokens to the bucket.
     */
    private void refill() {
        Instant now = Instant.now();

        // Geçen süreye göre eklenecek jeton miktarını hesapla
        // Calculate how many tokens to add based on the time elapsed
        double tokensToAdd = (now.toEpochMilli() - lastRefillTimestamp.toEpochMilli()) * fillRate / 1000.0;

        // Jetonları ekle ama kapasiteyi aşma | Add tokens, but don't exceed capacity
        this.tokens = Math.min(capacity, this.tokens + tokensToAdd);

        // Son dolum zamanını güncelle | Update the last refill timestamp
        this.lastRefillTimestamp = now;
    }
}