package in.arpit.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import in.arpit.dto.BookingRequest;
import in.arpit.dto.BookingResponseDTO;
import in.arpit.security.AccessGuard;
import in.arpit.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AccessGuard guard;

    @PostMapping("/book")
    public ResponseEntity<BookingResponseDTO> bookParking(@RequestBody BookingRequest request) {
        if (!guard.isAdmin()) {
            // A driver can only book for themselves: ignore whatever driverId the client sent.
            request.setDriverId(guard.currentDriverId());
        }
        return ResponseEntity.ok(bookingService.bookParking(request));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByDriver(@PathVariable Long driverId) {
        guard.requireDriverAccess(driverId);
        return ResponseEntity.ok(bookingService.getBookingsByDriver(driverId));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByOwner(@PathVariable Long ownerId) {
        guard.requireOwnerAccess(ownerId);
        return ResponseEntity.ok(bookingService.getBookingsByOwner(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingbyId(id));
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}