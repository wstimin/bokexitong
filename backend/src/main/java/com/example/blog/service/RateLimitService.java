package com.example.blog.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.resetAt)) {
                return new Bucket(1, now.plus(window));
            }
            current.count += 1;
            return current;
        });
        if (bucket.count > maxRequests) {
            throw new IllegalArgumentException("操作过于频繁，请稍后再试");
        }
        cleanup(now);
    }

    private void cleanup(Instant now) {
        if (buckets.size() < 10000) return;
        buckets.entrySet().removeIf(entry -> now.isAfter(entry.getValue().resetAt));
    }

    private static class Bucket {
        private int count;
        private final Instant resetAt;

        private Bucket(int count, Instant resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }
}
