package in.arpit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    
    private LocalDate bookingDate;

    @NotNull(message = "Start time required hai")
    @Column(nullable = false)
    private LocalTime startTime;

   
    private LocalTime endTime;

 
  private Integer durationHours;

   
    @Builder.Default
    private String status = "ACTIVE";

    
    private Double totalCost;

   
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parking_id", nullable = false)
    private Parking parking;

    
}
