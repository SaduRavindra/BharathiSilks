package com.bharathisilks.service;

/**
 * Outbound SMS abstraction for OTP delivery. The default {@link LogSmsSender}
 * just logs; a real provider (Twilio, MSG91, …) can be dropped in later behind
 * configuration without touching the auth flow.
 */
public interface SmsSender {
    void send(String phone, String message);
}
