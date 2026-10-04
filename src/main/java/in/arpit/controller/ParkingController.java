package in.arpit.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.arpit.dto.ApiResponse;
import in.arpit.dto.NearbyParkingDTO;
import in.arpit.dto.ParkingRequestDTO;
import in.arpit.dto.ParkingResponseDTO;
import in.arpit.dto.ParkingUpdateRequest;
import in.arpit.entity.Parking;
import in.arpit.security.AccessGuard;
import in.arpit.service.LocationService;
import in.arpit.service.ParkingService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/parkings")
public class ParkingController {

    @Autowired
    private ParkingService parkingServ;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AccessGuard guard;

    private Parking toEntity(ParkingRequestDTO dto) {
        Parking parking = new Parking();
        parking.setParkingName(dto.getParkingName());
        parking.setLocation(dto.getLocation());
        parking.setTotalSlots(dto.getTotalSlots());
        parking.setPricePerHour(dto.getPricePerHour());
        parking.setLatitude(dto.getLatitude());
        parking.setLongitude(dto.getLongitude());
        return parking;
    }

    // ---------- existing endpoint (kept; now also accepts optional GPS fields) ----------
    @PostMapping("/add/{ownerId}")
    public ResponseEntity<ParkingResponseDTO> addParking(@PathVariable Long ownerId,
            @Valid @RequestBody ParkingRequestDTO dto) {
        guard.requireOwnerAccess(ownerId);   // an owner may only add parkings to their own profile
        return ResponseEntity.ok(parkingServ.addParking(ownerId, toEntity(dto)));
    }

    // ---------- new: owner is taken from the authenticated user, not from the client ----------
    @PostMapping
    public ResponseEntity<ParkingResponseDTO> createParking(@Valid @RequestBody ParkingRequestDTO dto) {
        Long ownerId = guard.currentOwnerId();   // from the JWT, never from the body
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingServ.addParking(ownerId, toEntity(dto)));
    }

    // ---------- new: update (incl. coordinates), ownership enforced ----------
    @PutMapping("/{id}")
    public ResponseEntity<ParkingResponseDTO> updateParking(@PathVariable Long id,
            @Valid @RequestBody ParkingUpdateRequest req) {
        return ResponseEntity.ok(parkingServ.updateParking(id, req));   // ownership checked in the service
    }

    // ---------- new: nearby search ----------
    // NOTE: must stay a literal path; Spring prefers it over "/{id}".
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyParkingDTO>>> getNearby(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Double radius,           // km, default 5
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        List<NearbyParkingDTO> data = locationService.findNearby(latitude, longitude, radius, availableOnly);
        return ResponseEntity.ok(ApiResponse.ok("Nearby parking locations fetched successfully", data));
    }

    @GetMapping
    public ResponseEntity<List<ParkingResponseDTO>> getAllParkings() {
        return ResponseEntity.ok(parkingServ.getAllParkings());
    }

    @GetMapping("/my-parkings/{ownerId}")
    public ResponseEntity<List<ParkingResponseDTO>> getParkingsByOwner(@PathVariable Long ownerId) {
        guard.requireOwnerAccess(ownerId);
        return ResponseEntity.ok(parkingServ.getParkingsByOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingResponseDTO> getParkingById(@PathVariable Long id) {
        return ResponseEntity.ok(parkingServ.getParkingById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ParkingResponseDTO>> searchByLocation(@RequestParam String location) {
        return ResponseEntity.ok(parkingServ.searchByLocation(location.trim()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ParkingResponseDTO>> getAvailable() {
        return ResponseEntity.ok(parkingServ.getAvailableParkings());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteParking(@PathVariable Long id) {
        return ResponseEntity.ok(parkingServ.ParkingByDelete(id));
    }
}
