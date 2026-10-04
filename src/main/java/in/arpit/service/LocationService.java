package in.arpit.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import in.arpit.dto.NearbyParkingDTO;
import in.arpit.entity.Parking;
import in.arpit.exception.InvalidLocationException;
import in.arpit.repository.ParkingRepository;
import in.arpit.util.DistanceCalculator;
import in.arpit.util.DistanceCalculator.BoundingBox;

/**
 * Single home for all location logic: coordinate validation + nearby search.
 * The caller's GPS position is used for the calculation only; it is never persisted or logged.
 */
@Service
public class LocationService {

    public static final double DEFAULT_RADIUS_KM = 5.0;
    public static final double MAX_RADIUS_KM = 100.0;

    private final ParkingRepository parkingRepo;

    public LocationService(ParkingRepository parkingRepo) {
        this.parkingRepo = parkingRepo;
    }

    /** Validates a single coordinate pair. Both null is allowed only when allowEmpty is true. */
    public void validateCoordinates(Double lat, Double lon, boolean allowEmpty) {
        if (lat == null && lon == null) {
            if (allowEmpty) {
                return;
            }
            throw new InvalidLocationException("latitude and longitude are required");
        }
        if (lat == null || lon == null) {
            throw new InvalidLocationException("latitude and longitude must be provided together");
        }
        if (lat.isNaN() || lat < -90.0 || lat > 90.0) {
            throw new InvalidLocationException("latitude must be between -90 and 90");
        }
        if (lon.isNaN() || lon < -180.0 || lon > 180.0) {
            throw new InvalidLocationException("longitude must be between -180 and 180");
        }
    }

    public List<NearbyParkingDTO> findNearby(Double lat, Double lon, Double radiusKm, boolean availableOnly) {
        validateCoordinates(lat, lon, false);

        double radius = (radiusKm == null) ? DEFAULT_RADIUS_KM : radiusKm;
        if (Double.isNaN(radius) || radius <= 0 || radius > MAX_RADIUS_KM) {
            throw new InvalidLocationException("radius must be greater than 0 and at most " + (int) MAX_RADIUS_KM + " km");
        }

        // 1) cheap DB pre-filter on a bounding box (parkings without coordinates are never returned)
        BoundingBox box = DistanceCalculator.boundingBox(lat, lon, radius);
        List<Parking> candidates = parkingRepo.findWithinBox(
                box.minLat(), box.maxLat(), box.minLon(), box.maxLon());

        // 2) exact Haversine distance, radius + availability filter, nearest first
        return candidates.stream()
                .map(p -> toNearby(p, DistanceCalculator.haversineKm(lat, lon, p.getLatitude(), p.getLongitude())))
                .filter(d -> d.distanceKm() <= radius)
                .filter(d -> !availableOnly || (d.dto().getAvailableSlots() != null && d.dto().getAvailableSlots() > 0))
                .sorted(Comparator.comparingDouble(Scored::distanceKm))
                .map(d -> {
                    d.dto().setDistanceKm(Math.round(d.distanceKm() * 100.0) / 100.0);
                    return d.dto();
                })
                .toList();
    }

    private record Scored(NearbyParkingDTO dto, double distanceKm) {
    }

    private Scored toNearby(Parking p, double distanceKm) {
        NearbyParkingDTO dto = new NearbyParkingDTO();
        dto.setId(p.getParkingId());
        dto.setParkingName(p.getParkingName() != null ? p.getParkingName() : p.getLocation());
        dto.setAddress(p.getLocation());
        dto.setLatitude(p.getLatitude());
        dto.setLongitude(p.getLongitude());
        dto.setTotalSlots(p.getTotalSlots());
        dto.setAvailableSlots(p.getAvailableSlots());
        dto.setPricePerHour(p.getPricePerHour());
        dto.setStatus(p.getStatus());
        return new Scored(dto, distanceKm);
    }
}
