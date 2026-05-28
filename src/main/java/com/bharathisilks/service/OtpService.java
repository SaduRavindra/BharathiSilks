package com.bharathisilks.service;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.web.dto.AuthResponse;
import com.bharathisilks.web.dto.OtpRequestResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Passwordless phone login. Generates short-lived one-time codes kept in memory.
 * There is no SMS gateway wired, so in dev the code is logged and (optionally)
 * returned to the caller; swap {@link #deliver} for a real sender in production.
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int MAX_ATTEMPTS = 5;

    private record Entry(String code, Instant expiresAt, int attempts) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private final UserService users;
    private final JwtService jwt;
    private final long ttlSeconds;
    private final boolean exposeCode;

    public OtpService(UserService users, JwtService jwt,
                      @Value("${otp.ttl-seconds}") long ttlSeconds,
                      @Value("${otp.expose-code}") boolean exposeCode) {
        this.users = users;
        this.jwt = jwt;
        this.ttlSeconds = ttlSeconds;
        this.exposeCode = exposeCode;
    }

    public OtpRequestResponse request(String rawPhone) {
        String phone = normalize(rawPhone);
        if (phone.length() < 7) {
            throw new IllegalArgumentException("Enter a valid phone number");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(phone, new Entry(code, Instant.now().plusSeconds(ttlSeconds), 0));
        deliver(phone, code);
        return new OtpRequestResponse(true, ttlSeconds, exposeCode ? code : null);
    }

    public AuthResponse verify(String rawPhone, String code) {
        String phone = normalize(rawPhone);
        Entry entry = store.get(phone);
        if (entry == null) {
            throw new IllegalArgumentException("Request a code first");
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phone);
            throw new IllegalArgumentException("Code expired — request a new one");
        }
        if (entry.attempts() >= MAX_ATTEMPTS) {
            store.remove(phone);
            throw new IllegalArgumentException("Too many attempts — request a new code");
        }
        if (code == null || !code.trim().equals(entry.code())) {
            store.put(phone, new Entry(entry.code(), entry.expiresAt(), entry.attempts() + 1));
            throw new IllegalArgumentException("Incorrect code");
        }
        store.remove(phone);
        AppUser user = users.upsertPhone(phone);
        return new AuthResponse(jwt.generate(user), user);
    }

    private void deliver(String phone, String code) {
        log.info("OTP for {} is {}", phone, code);
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }
}
