package in.arpit.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parkings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long parkingId;

    @NotBlank(message = "Location required hai")
    @Column(nullable = false)
    private String location;

    /** Optional display name (added for GPS feature). Nullable so old rows keep working. */
    @Column(nullable = true)
    private String parkingName;

    /** GPS latitude, -90..90. Nullable: legacy parkings have no coordinates. */
    @Column(nullable = true)
    private Double latitude;

    /** GPS longitude, -180..180. Nullable: legacy parkings have no coordinates. */
    @Column(nullable = true)
    private Double longitude;
   
    

    @NotNull(message = "Total slots required hai")
    @Min(value = 1, message = "Minimum 1 slot hona chahiye")
    @Column(nullable = false)
    private Integer totalSlots;

    @Column(nullable = false)
    private Integer availableSlots;

    @NotNull(message = "Price per hour required hai")
    @Min(value = 0, message = "Price negative nahi ho sakti")
    @Column(nullable = false)
    private Double pricePerHour;

   
    @Builder.Default
    private String status = "OPEN";

   
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

   
    @OneToMany(mappedBy = "parking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    
}
