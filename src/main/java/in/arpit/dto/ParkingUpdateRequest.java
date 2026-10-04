package in.arpit.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

/** Partial update: any field left null is not changed. Note there is deliberately NO ownerId. */
@Data
public class ParkingUpdateRequest {
    private String parkingName;
    private String location;
    private Integer totalSlots;
    private Double pricePerHour;
    private String status;

    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
    private Double longitude;
}
