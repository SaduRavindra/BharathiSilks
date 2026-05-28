package com.bharathisilks.web.dto;

/** {@code devCode} is populated only when no real SMS sender is configured. */
public record OtpRequestResponse(boolean sent, long expiresInSeconds, String devCode) {
}
