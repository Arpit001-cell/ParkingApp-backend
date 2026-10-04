package in.arpit.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.arpit.dto.AuthResponse;
import in.arpit.dto.DriverProfileRequest;
import in.arpit.dto.LoginRequest;
import in.arpit.dto.OwnerProfileRequest;
import in.arpit.dto.RegisterRequest;
import in.arpit.dto.UserResponse;
import in.arpit.entity.AuthProvider;
import in.arpit.entity.Driver;
import in.arpit.entity.Owner;
import in.arpit.entity.Role;
import in.arpit.entity.User;
import in.arpit.exception.BadRequestException;
import in.arpit.exception.ConflictException;
import in.arpit.exception.OAuth2LoginException;
import in.arpit.exception.UnauthorizedException;
import in.arpit.repository.DriverRepository;
import in.arpit.repository.OwnerRepository;
import in.arpit.repository.UserRepository;
import in.arpit.security.AppUserDetails;
import in.arpit.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final OwnerRepository ownerRepo;
    private final DriverRepository driverRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo, OwnerRepository ownerRepo, DriverRepository driverRepo,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepo = userRepo;
        this.ownerRepo = ownerRepo;
        this.driverRepo = driverRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // ------------------------------------------------------------------ register / login

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.getEmail());
        if (userRepo.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        Role role = req.getRole() == null ? Role.USER : req.getRole();
        if (role == Role.ADMIN) {
            throw new BadRequestException("ADMIN accounts cannot be self-registered");
        }

        User user = userRepo.save(User.builder()
                .name(req.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))   // BCrypt
                .role(role)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build());

        if (role == Role.OWNER) {
            createOwnerProfile(user, req.getPhone());
        } else if (role == Role.DRIVER) {
            createDriverProfile(user, req.getPhone(), req.getVehicleNumber());
        }

        return build("Registration successful", user);
    }

    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException (-> 401) on wrong email/password. Password is verified with BCrypt.
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalize(req.getEmail()), req.getPassword()));
        AppUserDetails principal = (AppUserDetails) auth.getPrincipal();
        User user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        return build("Login successful", user);
    }

    public UserResponse me(Long userId) {
        return toResponse(userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required")));
    }

    // ------------------------------------------------------------------ profiles (needed for Google users)

    /** Turns the logged-in USER into an OWNER with a linked Owner profile. Returns a fresh JWT. */
    @Transactional
    public AuthResponse becomeOwner(Long userId, OwnerProfileRequest req) {
        User user = loadForProfile(userId);
        createOwnerProfile(user, req.getPhone());
        user.setRole(Role.OWNER);
        return build("Owner profile created", userRepo.save(user));
    }

    @Transactional
    public AuthResponse becomeDriver(Long userId, DriverProfileRequest req) {
        User user = loadForProfile(userId);
        createDriverProfile(user, req.getPhone(), req.getVehicleNumber());
        user.setRole(Role.DRIVER);
        return build("Driver profile created", userRepo.save(user));
    }

    private User loadForProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
        if (user.getRole() != Role.USER) {
            throw new ConflictException("Account already has the role " + user.getRole());
        }
        return user;
    }

    private void createOwnerProfile(User user, String phone) {
        if (phone == null || !phone.matches("^[0-9]{10}$")) {
            throw new BadRequestException("A 10 digit phone number is required for an OWNER account");
        }
        if (ownerRepo.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered to another owner");
        }
        Owner owner = new Owner();
        owner.setOwnerName(user.getName());
        owner.setPhone(phone);
        owner.setUser(user);
        ownerRepo.save(owner);
    }

    private void createDriverProfile(User user, String phone, String vehicleNumber) {
        if (phone == null || phone.isBlank() || vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new BadRequestException("phone and vehicleNumber are required for a DRIVER account");
        }
        if (driverRepo.existsByVehicleNumber(vehicleNumber.trim())) {
            throw new ConflictException("Vehicle number is already registered");
        }
        if (driverRepo.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered to another driver");
        }
        Driver driver = new Driver();
        driver.setName(user.getName());
        driver.setVehicleNumber(vehicleNumber.trim());
        driver.setPhone(phone);
        driver.setUser(user);
        driverRepo.save(driver);
    }

    // ------------------------------------------------------------------ Google

    /**
     * Called by the OAuth2 success handler after Google has authenticated the person.
     * Returns our own JWT. Throws OAuth2LoginException(code) for anything we refuse.
     */
    @Transactional
    public String loginWithGoogle(String googleSub, String email, String name, boolean emailVerified) {
        if (googleSub == null || email == null) {
            throw new OAuth2LoginException("google_profile_incomplete");
        }
        if (!emailVerified) {
            throw new OAuth2LoginException("google_email_not_verified");
        }
        String normalized = normalize(email);

        User user = userRepo.findByProviderAndProviderId(AuthProvider.GOOGLE, googleSub).orElse(null);
        if (user == null) {
            User sameEmail = userRepo.findByEmail(normalized).orElse(null);
            if (sameEmail != null) {
                // Do NOT silently merge into an existing password account: someone could have pre-registered
                // this email locally (account pre-hijacking). The owner must sign in with their password.
                throw new OAuth2LoginException("account_exists_use_password_login");
            }
            user = userRepo.save(User.builder()
                    .name(name == null || name.isBlank() ? normalized : name.trim())
                    .email(normalized)
                    .password(null)                 // Google accounts have no password
                    .role(Role.USER)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(googleSub)
                    .enabled(true)
                    .build());
        }
        if (!user.isEnabled()) {
            throw new OAuth2LoginException("account_disabled");
        }
        return jwtService.generateToken(user);
    }

    // ------------------------------------------------------------------ helpers

    private AuthResponse build(String message, User user) {
        return new AuthResponse(true, message, jwtService.generateToken(user), toResponse(user));
    }

    private UserResponse toResponse(User u) {
        Long ownerId = ownerRepo.findByUser_Id(u.getId()).map(Owner::getOwnerId).orElse(null);
        Long driverId = driverRepo.findByUser_Id(u.getId()).map(Driver::getId).orElse(null);
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(),
                u.getProvider().name(), ownerId, driverId);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
