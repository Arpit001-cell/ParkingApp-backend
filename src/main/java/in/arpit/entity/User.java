package in.arpit.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/** Authentication identity. Kept separate from Driver/Owner so the parking domain is untouched. */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = { "provider", "provider_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;          // always stored lower-case

    /** BCrypt hash. NULL for Google accounts (they cannot log in with a password). */
    @Column(length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    /** Google "sub" claim for GOOGLE users, null for LOCAL. */
    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
