package com.finance_tracker.utils.security;

import com.finance_tracker.exception.BusinessLogicException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.getField;

class LoginRateLimiterTest {

    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new LoginRateLimiter();
    }

    @Test
    void checkAndRecord_allowsUpToFiveAttempts() {
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.checkAndRecord("127.0.0.1")).doesNotThrowAnyException();
        }
    }

    @Test
    void checkAndRecord_throwsOnSixthAttempt() {
        for (int i = 0; i < 5; i++) {
            limiter.checkAndRecord("10.0.0.1");
        }
        assertThatThrownBy(() -> limiter.checkAndRecord("10.0.0.1"))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessageContaining("Too many login attempts");
    }

    @Test
    void clearAttempts_resetsCountForIp() {
        for (int i = 0; i < 5; i++) limiter.checkAndRecord("1.2.3.4");
        limiter.clearAttempts("1.2.3.4");
        assertThatCode(() -> limiter.checkAndRecord("1.2.3.4")).doesNotThrowAnyException();
    }

    @Test
    void cleanupStaleEntries_removesExpiredIps() {
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, List<Instant>> attempts =
                (ConcurrentHashMap<String, List<Instant>>) getField(limiter, "attempts");

        List<Instant> stale = new ArrayList<>();
        stale.add(Instant.now().minusMillis(120_000)); // older than the 60s window
        attempts.put("stale-ip", stale);

        limiter.cleanupStaleEntries();

        assertThat(attempts.containsKey("stale-ip")).isFalse();
    }
}
