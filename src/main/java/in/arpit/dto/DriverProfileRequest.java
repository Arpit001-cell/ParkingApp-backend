package in.arpit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DriverProfileRequest {
    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "phone must be 10 to 15 digits")
    private String phone;

    @NotBlank(message = "vehicleNumber is required")
    private String vehicleNumber;
}
