package com.scaler.orderprocessor.dto.order;

import jakarta.validation.constraints.NotBlank;
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
public class AddressDTO {
    @NotBlank
    @Size(max = 200)
    private String recipientName;

    @NotBlank
    @Size(max = 255)
    private String line1;

    @Size(max = 255)
    private String line2;

    @NotBlank
    @Size(max = 128)
    private String city;

    @Size(max = 128)
    private String stateRegion;

    @NotBlank
    @Size(max = 32)
    private String postalCode;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryCode;

    @Size(max = 32)
    private String phone;
}
