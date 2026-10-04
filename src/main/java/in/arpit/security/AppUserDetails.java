package in.arpit.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import in.arpit.entity.Role;
import in.arpit.entity.User;
import lombok.Getter;

/** The authenticated principal placed in the SecurityContext. */
@Getter
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;   // BCrypt hash, may be null for Google accounts
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Exactly one "ROLE_" prefix. hasRole("OWNER") adds the prefix itself, so never store "ROLE_" in the enum.
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
