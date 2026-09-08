package com.scaler.usermanagement.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
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
public class AddressResponseDTO {
    private UUID id;
    private String addressType;
    private String recipientName;
    private String line1;
    private String line2;
    private String city;
    private String stateRegion;
    private String postalCode;
    private String countryCode;
    private String phone;
    @JsonProperty("isDefault")
    private boolean isDefault;
}
