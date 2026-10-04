package in.arpit.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import in.arpit.dto.BookingRequest;
import in.arpit.dto.BookingResponseDTO;
import in.arpit.entity.Booking;
import in.arpit.entity.Driver;
import in.arpit.entity.Parking;
import in.arpit.exception.BookingNotFoundException;
import in.arpit.exception.DriverNotFoundException;
import in.arpit.exception.ParkingNotFoundException;
import in.arpit.repository.BookingRepository;
import in.arpit.repository.DriverRepository;
import in.arpit.repository.ParkingRepository;
import in.arpit.security.AccessGuard;
import in.arpit.exception.ForbiddenException;

@Service
public class BookingService {

    private final BookingRepository bookrepo;
    private final DriverRepository driverrepo;
    private final ParkingRepository parkingrepo;
    private final AccessGuard guard;

    public BookingService(BookingRepository bookrepo,
                          DriverRepository driverrepo,
                          ParkingRepository parkingrepo,
                          AccessGuard guard) {
        this.guard = guard;

        this.bookrepo = bookrepo;
        this.driverrepo = driverrepo;
        this.parkingrepo = parkingrepo;
    }

    /** Visible to: ADMIN, the driver who made it, or the owner of the booked parking. */
    private void requireBookingAccess(Booking b) {
        if (guard.isAdmin()) {
            return;
        }
        boolean isDriver = guard.driverId()
                .map(id -> b.getDriver() != null && id.equals(b.getDriver().getId())).orElse(false);
        boolean isOwner = guard.ownerId()
                .map(id -> b.getParking() != null && b.getParking().getOwner() != null
                        && id.equals(b.getParking().getOwner().getOwnerId())).orElse(false);
        if (!isDriver && !isOwner) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }
    }

    private BookingResponseDTO toResponse(Booking b) {

        BookingResponseDTO response = new BookingResponseDTO();

        response.setBookingId(b.getBookingId());
        response.setBookingDate(b.getBookingDate());
        response.setStartTime(b.getStartTime());
        response.setEndTime(b.getEndTime());
        response.setDurationHours(b.getDurationHours());
        response.setStatus(b.getStatus());
        response.setTotalCost(b.getTotalCost());

        if (b.getDriver() != null) {
            response.setDriverId(b.getDriver().getId());
            response.setDriverName(b.getDriver().getName());
            response.setVehicleNumber(b.getDriver().getVehicleNumber());
        }

        if (b.getParking() != null) {
            response.setParkingId(b.getParking().getParkingId());
            response.setParkingLocation(b.getParking().getLocation());
        }

        return response;
    }


    public BookingResponseDTO bookParking(BookingRequest request) {

        Driver driver = driverrepo.findById(request.getDriverId()).orElse(null);

        if (driver == null) {
            throw new DriverNotFoundException("Driver not found");
        }

        Parking parking = parkingrepo.findById(request.getParkingId()).orElse(null);

        if (parking == null) {
            throw new ParkingNotFoundException("Parking not found");
        }

        Booking booking = new Booking();

        booking.setDriver(driver);
        booking.setParking(parking);
        booking.setBookingDate(LocalDate.now());
        booking.setStatus("ACTIVE");
        booking.setStartTime(request.getStartTime());
        booking.setDurationHours(request.getDurationHours());

        double totalCost =
                request.getDurationHours() * parking.getPricePerHour();

        booking.setTotalCost(totalCost);

        parking.setAvailableSlots(parking.getAvailableSlots() - 1);

        parkingrepo.save(parking);

        Booking saved = bookrepo.save(booking);

        return toResponse(saved);
    }


    public List<BookingResponseDTO> getBookingsByOwner(Long ownerId) {

        List<Booking> bookings =
                bookrepo.findByParking_Owner_OwnerId(ownerId);

        if (bookings.isEmpty()) {
            throw new BookingNotFoundException(
                    "No bookings found for owner id : " + ownerId);
        }

        List<BookingResponseDTO> result = new ArrayList<>();

        for (Booking b : bookings) {
            result.add(toResponse(b));
        }

        return result;
    }


    public List<BookingResponseDTO> getBookingsByDriver(Long driverId) {

        List<Booking> bookings =
                bookrepo.findByDriver_Id(driverId);

        if (bookings.isEmpty()) {
            throw new BookingNotFoundException(
                    "No bookings found for driver id : " + driverId);
        }

        List<BookingResponseDTO> result = new ArrayList<>();

        for (Booking b : bookings) {
            result.add(toResponse(b));
        }

        return result;
    }


    public BookingResponseDTO getBookingbyId(Long id) {

        Booking booking = bookrepo.findById(id).orElse(null);

        if (booking == null) {
            throw new BookingNotFoundException(
                    "Booking not found with id : " + id);
        }

        requireBookingAccess(booking);

        return toResponse(booking);
    }


    public BookingResponseDTO cancelBooking(Long id) {

        Booking booking = bookrepo.findById(id).orElse(null);

        if (booking == null) {
            throw new BookingNotFoundException(
                    "Booking not found with id : " + id);
        }

        requireBookingAccess(booking);

        booking.setStatus("CANCELLED");

        LocalTime endTime =
                booking.getStartTime().plusHours(booking.getDurationHours());

        booking.setEndTime(endTime);

        if (booking.getTotalCost() == null) {

            double cost =
                    booking.getDurationHours()
                    * booking.getParking().getPricePerHour();

            booking.setTotalCost(cost);
        }

        Parking parking = booking.getParking();

        parking.setAvailableSlots(parking.getAvailableSlots() + 1);

        parkingrepo.save(parking);

        Booking updated = bookrepo.save(booking);

        return toResponse(updated);
    }
}