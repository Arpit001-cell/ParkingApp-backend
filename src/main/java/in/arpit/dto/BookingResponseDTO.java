
package in.arpit.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDTO {
    private Long bookingId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationHours;
    private String status;
    private Double totalCost;

    private Long driverId;
    private String driverName;
    private String vehicleNumber;

    private Long parkingId;
    private String parkingLocation;
}