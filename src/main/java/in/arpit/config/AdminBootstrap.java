package in.arpit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import in.arpit.entity.AuthProvider;
import in.arpit.entity.Role;
import in.arpit.entity.User;
import in.arpit.repository.UserRepository;

/** Creates the first ADMIN from environment variables (ADMIN_EMAIL / ADMIN_PASSWORD). Does nothing if unset. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepo, PasswordEncoder encoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.length() < 8) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepo.existsByEmail(normalized)) {
            return;
        }
        userRepo.save(User.builder()
                .name("Administrator")
                .email(normalized)
                .password(encoder.encode(password))
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build());
    }
}
