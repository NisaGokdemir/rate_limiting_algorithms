package org.kafka.rate_limiting;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Leaky Bucket (Sızdıran Kova) Algoritması Uygulaması
 * Trafiği düzene sokmak ve sabit bir çıkış hızı sağlamak için kullanılır.
 */
public class LeakyBucket {
    private final long capacity;        // Kovanın alabileceği maksimum istek sayısı | Maximum number of requests the bucket can hold
    private final double leakRate;      // İsteklerin kovadan sızma hızı (saniyedeki istek) | Rate at which requests leak out of the bucket (requests per second)
    private final Queue<Instant> bucket; // İsteklerin zaman damgalarını tutan kuyruk | Queue to hold timestamps of requests
    private Instant lastLeakTimestamp;   // Kovadan en son sızma gerçekleşen zaman | Last time we leaked from the bucket

    public LeakyBucket(long capacity, double leakRate) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.bucket = new LinkedList<>();
        this.lastLeakTimestamp = Instant.now();
    }

    /**
     * Yeni bir isteğin işleme alınıp alınamayacağını kontrol eder.
     * Checks if the new request can be accepted into the bucket.
     */
    public synchronized boolean allowRequest() {
        leak();  // Önce geçen süreye göre eski istekleri "sızdır" (temizle) | First, leak out any requests based on elapsed time

        // Kova kapasitesi dolmadıysa yeni isteği sıraya ekle | If bucket has room, add the new request
        if (bucket.size() < capacity) {
            bucket.offer(Instant.now());  // İsteği kovaya ekle | Add the new request to the bucket
            return true;  // İsteğe izin ver | Allow the request
        }

        return false;  // Kova dolu, isteği reddet | Bucket is full, deny the request
    }

    /**
     * Zamanın geçişine bağlı olarak istekleri kovadan tahliye eder.
     * Removes the appropriate number of requests from the bucket based on elapsed time.
     */
    private void leak() {
        Instant now = Instant.now();
        long elapsedMillis = now.toEpochMilli() - lastLeakTimestamp.toEpochMilli();

        // Geçen süre zarfında kaç isteğin "sızması" gerektiğini hesapla
        // Calculate how many items should have leaked
        int leakedItems = (int) (elapsedMillis * leakRate / 1000.0);

        // Hesaplanan miktar kadar isteği kuyruktan (kovadan) çıkar
        // Remove the leaked items from the bucket
        for (int i = 0; i < leakedItems && !bucket.isEmpty(); i++) {
            bucket.poll();
        }

        // Eğer sızma gerçekleştiyse zaman damgasını güncelle
        // Update the timestamp if any leak occurred
        if (leakedItems > 0) {
            lastLeakTimestamp = now;
        }
    }
}
