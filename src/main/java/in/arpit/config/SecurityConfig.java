package in.arpit.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import in.arpit.security.CustomUserDetailsService;
import in.arpit.security.JwtAuthenticationFilter;
import in.arpit.security.JwtService;
import in.arpit.security.RestSecurityHandlers;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // =========================
    // Password Encoder
    // =========================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================
    // Authentication Manager
    // =========================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    // =========================
    // Security Filter Chain
    // =========================
    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            RestSecurityHandlers restHandlers,

            @Qualifier("corsConfigurationSource")
            CorsConfigurationSource corsSource

    ) throws Exception {

        http

            // =========================
            // CSRF
            // =========================
            .csrf(AbstractHttpConfigurer::disable)

            // =========================
            // CORS
            // =========================
            .cors(cors ->
                cors.configurationSource(corsSource)
            )

            // =========================
            // Session Management
            // =========================
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // =========================
            // Exception Handling
            // =========================
            .exceptionHandling(exception ->
                exception
                    .authenticationEntryPoint(restHandlers)
                    .accessDeniedHandler(restHandlers)
            )

            // =========================
            // Authorization
            // =========================
            .authorizeHttpRequests(auth -> auth

                // -------------------------
                // OPTIONS / CORS
                // -------------------------
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                // -------------------------
                // Error
                // -------------------------
                .requestMatchers(
                    "/error"
                ).permitAll()

                // -------------------------
                // Authentication
                // -------------------------
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()

                // -------------------------
                // OAuth2 paths
                // Kept public for future
                // Google OAuth integration
                // -------------------------
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                // -------------------------
                // Static pages
                // -------------------------
                .requestMatchers(
                    "/login.html",
                    "/map.html",
                    "/favicon.ico"
                ).permitAll()

                // -------------------------
                // Legacy Driver / Owner
                // -------------------------
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/drivers/register",
                    "/api/drivers/login",
                    "/api/owners/register",
                    "/api/owners/login"
                ).permitAll()

                // =========================
                // OWNER / ADMIN
                // =========================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/parkings",
                    "/api/parkings/add/**"
                ).hasAnyRole(
                    "OWNER",
                    "ADMIN"
                )

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/parkings/**"
                ).hasAnyRole(
                    "OWNER",
                    "ADMIN"
                )

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/parkings/**"
                ).hasAnyRole(
                    "OWNER",
                    "ADMIN"
                )

                // =========================
                // DRIVER / ADMIN
                // =========================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/bookings/book"
                ).hasAnyRole(
                    "DRIVER",
                    "ADMIN"
                )

                // =========================
                // ADMIN ONLY
                // =========================

                .requestMatchers(
                    "/api/owners/phone/**",
                    "/api/drivers/vehicle/**"
                ).hasRole("ADMIN")

                // =========================
                // EVERYTHING ELSE
                // =========================
                .anyRequest().authenticated()
            )

            // =========================
            // JWT Authentication Filter
            // =========================
            .addFilterBefore(
                new JwtAuthenticationFilter(
                    jwtService,
                    userDetailsService
                ),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}