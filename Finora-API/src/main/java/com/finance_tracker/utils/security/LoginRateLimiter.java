package com.finance_tracker.utils.security;

import com.finance_tracker.exception.BusinessLogicException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000;

    private final ConcurrentHashMap<String, List<Instant>> attempts = new ConcurrentHashMap<>();

    public void checkAndRecord(String ip) {
        Instant now = Instant.now();
        long windowStart = now.toEpochMilli() - WINDOW_MILLIS;

        attempts.compute(ip, (key, timestamps) -> {
            if (timestamps == null) {
                timestamps = new ArrayList<>();
            }
            timestamps.removeIf(t -> t.toEpochMilli() < windowStart);
            timestamps.add(now);
            return timestamps;
        });

        int count = attempts.get(ip).size();
        if (count > MAX_ATTEMPTS) {
            throw new BusinessLogicException(
                "Too many login attempts. Please try again in a minute.");
        }
    }

    public void clearAttempts(String ip) {
        attempts.remove(ip);
    }
}
