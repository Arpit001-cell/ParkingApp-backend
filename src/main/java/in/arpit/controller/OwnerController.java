package in.arpit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import in.arpit.dto.OwnerRequestDTO;
import in.arpit.dto.OwnerResponseDTO;
import in.arpit.entity.Owner;
import in.arpit.security.AccessGuard;
import in.arpit.service.OwnerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private AccessGuard guard;

    /** LEGACY (public): creates an Owner profile NOT linked to a login. Prefer POST /api/auth/register with role OWNER. */
    @PostMapping("/register")
    public ResponseEntity<OwnerResponseDTO> registerOwner(@RequestBody OwnerRequestDTO dto) {

        Owner owner = new Owner();
        owner.setOwnerName(dto.getOwnerName());
        owner.setPhone(dto.getPhone());

        return ResponseEntity.ok(ownerService.registerOwner(owner));
    }
    /** LEGACY (public, deprecated): returns the profile only, no token. Use POST /api/auth/login. */
    @PostMapping("/login")
    public ResponseEntity<OwnerResponseDTO> loginOwner(@RequestBody OwnerRequestDTO dto) {
        return ResponseEntity.ok(ownerService.getOwnerByPhone(dto.getPhone()));
    }
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<OwnerResponseDTO> getOwnerById(@PathVariable Long id) {
        guard.requireOwnerAccess(id);

        return ResponseEntity.ok(ownerService.getOwnerById(id));
    }

    @GetMapping("/phone/{phone}")   // ADMIN only (see SecurityConfig)
    public ResponseEntity<OwnerResponseDTO> getOwnerByPhone(@PathVariable String phone) {

        return ResponseEntity.ok(ownerService.getOwnerByPhone(phone));
    }

    @PutMapping("/update/{id}")
    @Transactional
    public ResponseEntity<OwnerResponseDTO> updateOwner(@PathVariable Long id,
            @Valid @RequestBody OwnerRequestDTO dto) {
        guard.requireOwnerAccess(id);

        Owner owner = new Owner();
        owner.setOwnerName(dto.getOwnerName());
        owner.setPhone(dto.getPhone());

        return ResponseEntity.ok(ownerService.updateOwner(id, owner));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteOwner(@PathVariable Long id) {
        guard.requireOwnerAccess(id);

        return ResponseEntity.ok(ownerService.deleteOwner(id));
    }
}




