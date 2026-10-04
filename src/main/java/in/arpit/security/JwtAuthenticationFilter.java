package in.arpit.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads "Authorization: Bearer <jwt>", validates it and populates the SecurityContext.
 * Deliberately NOT a @Component: it is created inside SecurityConfig so Spring Boot does not also
 * register it as a plain servlet filter (which would run it twice per request).
 * An invalid token simply leaves the request unauthenticated; the entry point then answers 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                Jwt jwt = jwtService.parse(token);
                UserDetails user = userDetailsService.loadUserByUsername(jwt.getSubject());
                if (user.isEnabled()) {
                    UsernamePasswordAuthenticationToken auth =
                            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();   // bad/expired token -> stay anonymous -> 401
            }
        }
        chain.doFilter(request, response);
    }
}
