package in.arpit.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import in.arpit.exception.OAuth2LoginException;
import in.arpit.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Google has authenticated the user. Find/create the local user, mint OUR JWT and redirect to the frontend.
 * The JWT goes in the URL fragment (#token=...) so it is never sent to a server or written to access logs.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final AuthService authService;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(AuthService authService,
                                              @Value("${app.oauth2.redirect-uri}") String redirectUri) {
        this.authService = authService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String fragment;
        try {
            OAuth2User google = (OAuth2User) authentication.getPrincipal();
            String sub = google.getAttribute("sub");
            String email = google.getAttribute("email");
            String name = google.getAttribute("name");
            Boolean verified = google.getAttribute("email_verified");

            String token = authService.loginWithGoogle(sub, email, name, Boolean.TRUE.equals(verified));
            fragment = "token=" + token;
        } catch (OAuth2LoginException e) {
            fragment = "error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            log.error("Google login failed while creating the local session: {}", e.getClass().getSimpleName());
            fragment = "error=login_failed";
        }

        // The temporary session only existed to carry the OAuth "state"; we are stateless afterwards.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(UriComponentsBuilder.fromUriString(redirectUri).fragment(fragment).build().toUriString());
    }
}
