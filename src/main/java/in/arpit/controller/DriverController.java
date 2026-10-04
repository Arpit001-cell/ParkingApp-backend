package in.arpit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.arpit.dto.DriverRequestDTO;
import in.arpit.dto.DriverResponseDTO;
import in.arpit.entity.Driver;
import in.arpit.security.AccessGuard;
import in.arpit.service.DriverService;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @Autowired
    private AccessGuard guard;

    /** LEGACY (public): creates a Driver profile NOT linked to a login. Prefer POST /api/auth/register with role DRIVER. */
    @PostMapping("/register")
    public ResponseEntity<DriverResponseDTO> registerDriver(@RequestBody DriverRequestDTO dto) {
        return ResponseEntity.ok(driverService.registerDriver(dto));
    }

    /** LEGACY (public, deprecated): returns the profile only, no token. Use POST /api/auth/login. */
    @PostMapping("/login")
    public ResponseEntity<DriverResponseDTO> loginDriver(@RequestBody DriverRequestDTO dto) {
        Driver driver = driverService.loginDriver(dto.getVehicleNumber());
        return ResponseEntity.ok(driverService.getDriverById(driver.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDTO> getDriverById(@PathVariable Long id) {
        guard.requireDriverAccess(id);
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @GetMapping("/vehicle/{vehicleNumber}")   // ADMIN only (see SecurityConfig)
    public ResponseEntity<DriverResponseDTO> getDriverByVehicle(@PathVariable String vehicleNumber) {
        return ResponseEntity.ok(driverService.getDriverByVehicle(vehicleNumber));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<DriverResponseDTO> updateDriver(@PathVariable Long id,
                                                           @RequestBody DriverRequestDTO dto) {
        guard.requireDriverAccess(id);
        return ResponseEntity.ok(driverService.updateDriver(id, dto));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteDriver(@PathVariable Long id) {
        guard.requireDriverAccess(id);
        return ResponseEntity.ok(driverService.deleteDriver(id));
    }
}


