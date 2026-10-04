package in.arpit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OwnerProfileRequest {
    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "owner phone must be exactly 10 digits")
    private String phone;
}
