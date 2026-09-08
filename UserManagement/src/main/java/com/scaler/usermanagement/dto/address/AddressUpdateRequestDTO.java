package com.scaler.usermanagement.dto.address;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressUpdateRequestDTO {

    @Size(min = 1, max = 200)
    private String recipientName;

    @Size(min = 1, max = 255)
    private String line1;

    @Size(max = 255)
    private String line2;

    @Size(min = 1, max = 128)
    private String city;

    @Size(max = 128)
    private String stateRegion;

    @Size(min = 1, max = 32)
    private String postalCode;

    private Boolean isDefault;
}
