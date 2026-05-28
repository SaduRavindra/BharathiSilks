package com.bharathisilks.service;

import com.bharathisilks.web.dto.AuthResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Short-lived, single-use codes handed to the browser after a Google redirect,
 * so the JWT itself never travels in the URL/browser history. The SPA swaps the
 * code for the token via POST /api/auth/exchange.
 */
@Service
public class LoginCodeService {

    private record Entry(String token, String subject, Instant expiresAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final UserService users;
    private final long ttlSeconds;

    public LoginCodeService(UserService users,
                            @Value("${auth.login-code.ttl-seconds:120}") long ttlSeconds) {
        this.users = users;
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(String token, String subject) {
        purgeExpired();
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        store.put(code, new Entry(token, subject, Instant.now().plusSeconds(ttlSeconds)));
        return code;
    }

    public AuthResponse exchange(String code) {
        Entry entry = code == null ? null : store.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new IllegalArgumentException("Invalid or expired sign-in code");
        }
        return new AuthResponse(entry.token(), users.bySubject(entry.subject()));
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        store.values().removeIf(e -> now.isAfter(e.expiresAt()));
    }
}
