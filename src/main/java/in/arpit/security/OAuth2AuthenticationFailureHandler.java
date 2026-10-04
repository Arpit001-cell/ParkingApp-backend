package in.arpit.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Google login failed or was cancelled. Send the user back to the frontend with a generic error code. */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final String redirectUri;

    public OAuth2AuthenticationFailureHandler(@Value("${app.oauth2.redirect-uri}") String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // Class name only: never log the exception message/details (may contain OAuth parameters).
        log.warn("Google OAuth2 login failed: {}", exception.getClass().getSimpleName());
        response.sendRedirect(UriComponentsBuilder.fromUriString(redirectUri)
                .fragment("error=google_login_failed").build().toUriString());
    }
}
