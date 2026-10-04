package in.arpit.dto;

import in.arpit.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name is too long")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email is not valid")
    @Size(max = 254, message = "email is too long")
    private String email;

    // BCrypt only uses the first 72 bytes, so cap the length.
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
    private String password;

    /** Optional: USER (default), DRIVER or OWNER. ADMIN can never be self-assigned. */
    private Role role;

    /** Required for OWNER (exactly 10 digits) and DRIVER (10-15 digits). */
    @Pattern(regexp = "^[0-9]{10,15}$", message = "phone must be 10 to 15 digits")
    private String phone;

    /** Required for DRIVER. */
    private String vehicleNumber;
}
