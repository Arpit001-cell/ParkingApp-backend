package in.arpit.dto;

import java.time.LocalTime;

import lombok.Data;

@Data
public class BookingRequest {
	 private Long driverId;
	    private Long parkingId;
	    private LocalTime startTime;
	    private Integer durationHours;

}
