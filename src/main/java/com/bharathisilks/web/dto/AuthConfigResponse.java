package com.bharathisilks.web.dto;

/** Tells the login screen which methods are available. */
public record AuthConfigResponse(boolean googleEnabled, boolean otpDevMode) {
}
