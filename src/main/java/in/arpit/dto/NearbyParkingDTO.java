package in.arpit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyParkingDTO {
    private Long id;
    private String parkingName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private Integer totalSlots;
    private Integer availableSlots;
    private Double pricePerHour;
    private String status;
}
