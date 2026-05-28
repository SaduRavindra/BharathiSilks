package com.bharathisilks.web.dto;

import com.bharathisilks.domain.AppUser;

public record AuthResponse(String token, AppUser user) {
}
