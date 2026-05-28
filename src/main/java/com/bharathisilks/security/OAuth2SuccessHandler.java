package com.bharathisilks.security;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.service.JwtService;
import com.bharathisilks.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After Google authenticates the user, mint our own JWT and hand it back to the
 * single-page app via the URL fragment so the rest of the API stays stateless.
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService users;
    private final JwtService jwt;
    private final String redirect;

    public OAuth2SuccessHandler(UserService users, JwtService jwt,
                                @Value("${app.oauth2.redirect}") String redirect) {
        this.users = users;
        this.jwt = jwt;
        this.redirect = redirect;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        AppUser user = users.upsertGoogle(
                principal.getAttribute("sub"),
                principal.getAttribute("email"),
                principal.getAttribute("name"),
                principal.getAttribute("picture"));
        String token = jwt.generate(user);
        String target = redirect + "#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
