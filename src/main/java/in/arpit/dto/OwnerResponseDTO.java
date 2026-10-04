
package in.arpit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponseDTO {
    private Long ownerId;
    private String ownerName;
    private String phone;
}