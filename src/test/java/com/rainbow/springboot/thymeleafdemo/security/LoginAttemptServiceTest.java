package com.rainbow.springboot.thymeleafdemo.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

    @Test
    void blocksAfterMaxFailuresAndUnblocksAfterLockExpires() {
        long[] now = {1000};
        LoginAttemptService service = new LoginAttemptService(3, 60_000, () -> now[0]);

        service.recordFailure("alice");
        service.recordFailure("Alice");
        assertFalse(service.isBlocked("alice"));

        service.recordFailure("ALICE ");
        assertTrue(service.isBlocked("alice"));
        assertFalse(service.isBlocked("bob"));

        now[0] += 59_000;
        assertTrue(service.isBlocked("alice"));

        now[0] += 2_000;
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    void successfulLoginResetClearsFailures() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000, () -> 1000L);
        service.recordFailure("alice");
        service.recordFailure("alice");
        service.reset("alice");
        service.recordFailure("alice");
        assertFalse(service.isBlocked("alice"));
    }

    @Test
    void nullAndEmptyKeysAreSafe() {
        LoginAttemptService service = new LoginAttemptService(3, 60_000, () -> 1000L);
        service.recordFailure(null);
        assertFalse(service.isBlocked(null));
        assertFalse(service.isBlocked(""));
    }
}
