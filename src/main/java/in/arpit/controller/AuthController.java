package in.arpit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.arpit.dto.ApiResponse;
import in.arpit.dto.AuthResponse;
import in.arpit.dto.DriverProfileRequest;
import in.arpit.dto.LoginRequest;
import in.arpit.dto.OwnerProfileRequest;
import in.arpit.dto.RegisterRequest;
import in.arpit.dto.UserResponse;
import in.arpit.security.AccessGuard;
import in.arpit.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccessGuard guard;

    public AuthController(AuthService authService, AccessGuard guard) {
        this.authService = authService;
        this.guard = guard;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Who am I? Includes ownerId / driverId so the frontend can call the existing parking APIs. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok("Current user", authService.me(guard.currentUser().getId())));
    }

    /** For USER accounts (e.g. Google sign-ups): create an Owner profile, become OWNER, get a fresh token. */
    @PostMapping("/profile/owner")
    public ResponseEntity<AuthResponse> becomeOwner(@Valid @RequestBody OwnerProfileRequest req) {
        return ResponseEntity.ok(authService.becomeOwner(guard.currentUser().getId(), req));
    }

    @PostMapping("/profile/driver")
    public ResponseEntity<AuthResponse> becomeDriver(@Valid @RequestBody DriverProfileRequest req) {
        return ResponseEntity.ok(authService.becomeDriver(guard.currentUser().getId(), req));
    }
}
