package in.arpit.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import in.arpit.entity.Driver;
import in.arpit.entity.Owner;
import in.arpit.entity.Role;
import in.arpit.exception.ForbiddenException;
import in.arpit.exception.UnauthorizedException;
import in.arpit.repository.DriverRepository;
import in.arpit.repository.OwnerRepository;

/**
 * Answers "who is calling and what may they touch?" using the SecurityContext.
 * The caller's identity is NEVER taken from a request body or path variable.
 */
@Component
public class AccessGuard {

    private static final String DENIED = "You do not have permission to access this resource";

    private final OwnerRepository ownerRepo;
    private final DriverRepository driverRepo;

    public AccessGuard(OwnerRepository ownerRepo, DriverRepository driverRepo) {
        this.ownerRepo = ownerRepo;
        this.driverRepo = driverRepo;
    }

    public AppUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    public boolean isAdmin() {
        return currentUser().getRole() == Role.ADMIN;
    }

    public Optional<Long> ownerId() {
        return ownerRepo.findByUser_Id(currentUser().getId()).map(Owner::getOwnerId);
    }

    public Optional<Long> driverId() {
        return driverRepo.findByUser_Id(currentUser().getId()).map(Driver::getId);
    }

    /** The Owner profile linked to the logged-in account, or 403 if there is none. */
    public Long currentOwnerId() {
        return ownerId().orElseThrow(() -> new ForbiddenException("No owner profile is linked to this account"));
    }

    public Long currentDriverId() {
        return driverId().orElseThrow(() -> new ForbiddenException("No driver profile is linked to this account"));
    }

    /** Admin, or the owner whose profile id equals ownerId. */
    public void requireOwnerAccess(Long ownerId) {
        if (isAdmin()) {
            return;
        }
        if (!ownerId().map(id -> id.equals(ownerId)).orElse(false)) {
            throw new ForbiddenException(DENIED);
        }
    }

    /** Admin, or the driver whose profile id equals driverId. */
    public void requireDriverAccess(Long driverId) {
        if (isAdmin()) {
            return;
        }
        if (!driverId().map(id -> id.equals(driverId)).orElse(false)) {
            throw new ForbiddenException(DENIED);
        }
    }
}
