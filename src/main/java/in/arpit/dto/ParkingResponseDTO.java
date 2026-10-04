package in.arpit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingResponseDTO {
    private Long parkingId;
    private String location;
    private Integer totalSlots;
    private Integer availableSlots;
    private Double pricePerHour;
    private String status;
    private Long ownerId;
    private String ownerName;
    // GPS additions (null for parkings created before the GPS feature)
    private String parkingName;
    private Double latitude;
    private Double longitude;
}
