package in.arpit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Safe view of a user. Never contains the password hash. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String provider;
    private Long ownerId;    // set when the user has an Owner profile
    private Long driverId;   // set when the user has a Driver profile
}
