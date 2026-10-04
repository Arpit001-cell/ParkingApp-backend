package in.arpit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

@Data
public class ParkingRequestDTO {
    private String parkingName;      // optional
    private String location;         // human-readable address (existing field)
    private Integer totalSlots;
    private Double pricePerHour;

    // Optional GPS coordinates. If one is given, the other is required (checked in LocationService).
    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
    private Double longitude;
}
