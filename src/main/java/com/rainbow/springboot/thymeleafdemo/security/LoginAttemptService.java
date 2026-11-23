package com.rainbow.springboot.thymeleafdemo.security;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * In-memory brute-force protection for the login form.
 *
 * Failures are counted per key (the submitted username and the client IP are
 * tracked separately). Once a key reaches {@code maxAttempts} failures it is
 * blocked for {@code lockMillis}. A successful login clears the username key.
 *
 * LIMITATION: state is held in memory, so it resets on restart and is not shared
 * between multiple application instances. Use a shared store (e.g. Redis) or a
 * reverse-proxy rate limiter if you run more than one instance.
 */
@Service
public class LoginAttemptService {

    private static final class Entry {
        int failures;
        long lockedUntil;
        long lastFailure;
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockMillis;
    private final LongSupplier clock;

    @Autowired
    public LoginAttemptService(@Value("${app.security.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this(maxAttempts, lockMinutes * 60_000L, System::currentTimeMillis);
    }

    /** Test-friendly constructor with an injectable clock. */
    public LoginAttemptService(int maxAttempts, long lockMillis, LongSupplier clock) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockMillis = Math.max(1L, lockMillis);
        this.clock = clock;
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isBlocked(String key) {
        String k = normalize(key);
        if (k.isEmpty()) {
            return false;
        }
        Entry e = entries.get(k);
        if (e == null) {
            return false;
        }
        synchronized (e) {
            long now = clock.getAsLong();
            if (e.lockedUntil > now) {
                return true;
            }
            if (e.lockedUntil != 0) {
                // Lock expired: start fresh.
                e.lockedUntil = 0;
                e.failures = 0;
            }
            return false;
        }
    }

    public void recordFailure(String key) {
        String k = normalize(key);
        if (k.isEmpty()) {
            return;
        }
        Entry e = entries.computeIfAbsent(k, x -> new Entry());
        synchronized (e) {
            long now = clock.getAsLong();
            // Failures older than the lock window no longer count.
            if (e.lastFailure != 0 && now - e.lastFailure > lockMillis && e.lockedUntil <= now) {
                e.failures = 0;
            }
            e.failures++;
            e.lastFailure = now;
            if (e.failures >= maxAttempts) {
                e.lockedUntil = now + lockMillis;
            }
        }
    }

    public void reset(String key) {
        String k = normalize(key);
        if (!k.isEmpty()) {
            entries.remove(k);
        }
    }
}
