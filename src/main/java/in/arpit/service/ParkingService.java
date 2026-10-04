package in.arpit.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.arpit.dto.ParkingResponseDTO;
import in.arpit.entity.Owner;
import in.arpit.entity.Parking;
import in.arpit.dto.ParkingUpdateRequest;
import in.arpit.exception.ForbiddenException;
import in.arpit.exception.ParkingNotFoundException;
import in.arpit.security.AccessGuard;
import in.arpit.repository.OwnerRepository;
import in.arpit.repository.ParkingRepository;

@Service
public class ParkingService {

    private ParkingRepository parkingrepo;
    private OwnerRepository ownerrepo;
    private LocationService locationService;
    private AccessGuard guard;

    @Autowired
    public ParkingService(ParkingRepository parkingrepo, OwnerRepository ownerrepo,
            LocationService locationService, AccessGuard guard) {
        this.parkingrepo = parkingrepo;
        this.ownerrepo = ownerrepo;
        this.locationService = locationService;
        this.guard = guard;
    }

    private ParkingResponseDTO toResponse(Parking p) {

        ParkingResponseDTO response = new ParkingResponseDTO();

        response.setParkingId(p.getParkingId());
        response.setLocation(p.getLocation());
        response.setTotalSlots(p.getTotalSlots());
        response.setAvailableSlots(p.getAvailableSlots());
        response.setPricePerHour(p.getPricePerHour());
        response.setStatus(p.getStatus());
        response.setParkingName(p.getParkingName());
        response.setLatitude(p.getLatitude());
        response.setLongitude(p.getLongitude());

        if (p.getOwner() != null) {
            response.setOwnerId(p.getOwner().getOwnerId());
            response.setOwnerName(p.getOwner().getOwnerName());
        }

        return response;
    }

    @Transactional
    public ParkingResponseDTO addParking(Long ownerId, Parking p) {

        Owner owner = ownerrepo.findById(ownerId).orElse(null);

        if (owner == null) {
            throw new RuntimeException("Owner not found with id : " + ownerId);
        }

        locationService.validateCoordinates(p.getLatitude(), p.getLongitude(), true);

        p.setOwner(owner);
        p.setAvailableSlots(p.getTotalSlots());
        p.setStatus("OPEN");

        Parking saved = parkingrepo.save(p);

        return toResponse(saved);
    }

    /** Partial update. Only the owner of the parking (or an ADMIN) may change it; identity comes from the JWT. */
    @Transactional
    public ParkingResponseDTO updateParking(Long parkingId, ParkingUpdateRequest req) {

        Parking parking = parkingrepo.findById(parkingId)
                .orElseThrow(() -> new ParkingNotFoundException("Parking not found with id : " + parkingId));

        if (parking.getOwner() == null) {
            throw new ForbiddenException("You do not own this parking");
        }
        guard.requireOwnerAccess(parking.getOwner().getOwnerId());

        if (req.getLatitude() != null || req.getLongitude() != null) {
            locationService.validateCoordinates(req.getLatitude(), req.getLongitude(), false);
            parking.setLatitude(req.getLatitude());
            parking.setLongitude(req.getLongitude());
        }
        if (req.getParkingName() != null) {
            parking.setParkingName(req.getParkingName());
        }
        if (req.getLocation() != null && !req.getLocation().isBlank()) {
            parking.setLocation(req.getLocation());
        }
        if (req.getPricePerHour() != null) {
            if (req.getPricePerHour() < 0) {
                throw new RuntimeException("Price negative nahi ho sakti");
            }
            parking.setPricePerHour(req.getPricePerHour());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            parking.setStatus(req.getStatus());
        }
        if (req.getTotalSlots() != null) {
            if (req.getTotalSlots() < 1) {
                throw new RuntimeException("Minimum 1 slot hona chahiye");
            }
            int delta = req.getTotalSlots() - parking.getTotalSlots();
            parking.setTotalSlots(req.getTotalSlots());
            // keep already-booked slots booked: shift availability by the same delta, never below 0
            parking.setAvailableSlots(Math.max(0, parking.getAvailableSlots() + delta));
        }

        return toResponse(parkingrepo.save(parking));
    }

    public List<ParkingResponseDTO> getAllParkings() {

        List<Parking> parkings = parkingrepo.findAll();

        List<ParkingResponseDTO> result = new ArrayList<>();

        for (Parking p : parkings) {
            result.add(toResponse(p));
        }

        return result;
    }

    public List<ParkingResponseDTO> getParkingsByOwner(Long ownerId) {

        Owner owner = ownerrepo.findById(ownerId).orElse(null);

        if (owner == null) {
            throw new RuntimeException("Owner not found with id : " + ownerId);
        }

        List<Parking> parkings = parkingrepo.findByOwner_OwnerId(ownerId);

        List<ParkingResponseDTO> result = new ArrayList<>();

        for (Parking p : parkings) {
            result.add(toResponse(p));
        }

        return result;
    }

    public ParkingResponseDTO getParkingById(Long parkingId) {

        Parking parking = parkingrepo.findById(parkingId).orElse(null);

        if (parking == null) {
            throw new ParkingNotFoundException("Parking not found with id : " + parkingId);
        }

        return toResponse(parking);
    }

    public List<ParkingResponseDTO> searchByLocation(String location) {

        List<Parking> parkings = parkingrepo.findByLocationContainingIgnoreCase(location);

        if (parkings.isEmpty()) {
            throw new ParkingNotFoundException("Data not found. Please enter the correct location.");
        }

        List<ParkingResponseDTO> result = new ArrayList<>();

        for (Parking p : parkings) {
            result.add(toResponse(p));
        }

        return result;
    }

    public List<ParkingResponseDTO> getAvailableParkings() {

        List<Parking> parkings = parkingrepo.findByAvailableSlotsGreaterThan(0);

        List<ParkingResponseDTO> result = new ArrayList<>();

        for (Parking p : parkings) {
            result.add(toResponse(p));
        }

        return result;
    }

    public String ParkingByDelete(Long id) {

        Parking parking = parkingrepo.findById(id).orElse(null);

        if (parking == null) {
            throw new ParkingNotFoundException("Parking not found with id : " + id);
        }

        if (parking.getOwner() != null) {
            guard.requireOwnerAccess(parking.getOwner().getOwnerId());
        }

        parkingrepo.delete(parking);

        return "Parking deleted successfully with id : " + id;
    }
}