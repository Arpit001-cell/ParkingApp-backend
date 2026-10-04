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
import in.arpit.security.OAuth2AuthenticationFailureHandler;
import in.arpit.security.OAuth2AuthenticationSuccessHandler;
import in.arpit.security.RestSecurityHandlers;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Built from AuthenticationConfiguration.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            RestSecurityHandlers restHandlers,
            OAuth2AuthenticationSuccessHandler oauthSuccess,
            OAuth2AuthenticationFailureHandler oauthFailure,

            // IMPORTANT:
            // Explicitly tell Spring which CorsConfigurationSource to use
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
            .cors(cors -> cors.configurationSource(corsSource))

            // =========================
            // Session Management
            // =========================
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // =========================
            // Exception Handling
            // =========================
            .exceptionHandling(e -> e
                .authenticationEntryPoint(restHandlers)
                .accessDeniedHandler(restHandlers)
            )

            // =========================
            // Authorization
            // =========================
            .authorizeHttpRequests(auth -> auth

                // ---- Public ----
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers("/error").permitAll()

                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login"
                ).permitAll()

                // ---- Google OAuth2 ----
                .requestMatchers(
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()

                // ---- Static pages ----
                .requestMatchers(
                    "/login.html",
                    "/map.html",
                    "/favicon.ico"
                ).permitAll()

                // ---- Legacy Driver / Owner endpoints ----
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
                ).hasAnyRole("OWNER", "ADMIN")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/parkings/**"
                ).hasAnyRole("OWNER", "ADMIN")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/parkings/**"
                ).hasAnyRole("OWNER", "ADMIN")

                // =========================
                // DRIVER / ADMIN
                // =========================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/bookings/book"
                ).hasAnyRole("DRIVER", "ADMIN")

                // =========================
                // ADMIN ONLY
                // =========================

                .requestMatchers(
                    "/api/owners/phone/**",
                    "/api/drivers/vehicle/**"
                ).hasRole("ADMIN")

                // =========================
                // Everything else
                // =========================
                .anyRequest().authenticated()
            )

            // =========================
            // Google OAuth2 Login
            // =========================
            .oauth2Login(oauth -> oauth
                .successHandler(oauthSuccess)
                .failureHandler(oauthFailure)
            )

            // =========================
            // JWT Filter
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