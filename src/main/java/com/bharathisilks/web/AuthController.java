package com.bharathisilks.web;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.service.LoginCodeService;
import com.bharathisilks.service.OtpService;
import com.bharathisilks.service.UserService;
import com.bharathisilks.web.dto.AuthConfigResponse;
import com.bharathisilks.web.dto.AuthResponse;
import com.bharathisilks.web.dto.ExchangeRequest;
import com.bharathisilks.web.dto.OtpRequest;
import com.bharathisilks.web.dto.OtpRequestResponse;
import com.bharathisilks.web.dto.OtpVerifyRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OtpService otp;
    private final UserService users;
    private final LoginCodeService loginCodes;
    private final boolean googleEnabled;
    private final boolean otpDevMode;

    public AuthController(OtpService otp, UserService users, LoginCodeService loginCodes,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                          @Value("${otp.expose-code}") boolean otpDevMode) {
        this.otp = otp;
        this.users = users;
        this.loginCodes = loginCodes;
        this.googleEnabled = clientRegistrations.getIfAvailable() != null;
        this.otpDevMode = otpDevMode;
    }

    @GetMapping("/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(googleEnabled, otpDevMode);
    }

    @PostMapping("/otp/request")
    public OtpRequestResponse requestOtp(@RequestBody OtpRequest req) {
        return otp.request(req.phone());
    }

    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@RequestBody OtpVerifyRequest req) {
        return otp.verify(req.phone(), req.code());
    }

    @PostMapping("/exchange")
    public AuthResponse exchange(@RequestBody ExchangeRequest req) {
        return loginCodes.exchange(req.code());
    }

    @GetMapping("/me")
    public AppUser me(Authentication authentication) {
        return users.bySubject(authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Stateless: the client discards its token. Endpoint exists for symmetry.
    }
}
