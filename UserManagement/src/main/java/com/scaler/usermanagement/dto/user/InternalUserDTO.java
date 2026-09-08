package com.scaler.usermanagement.dto.user;

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
public class InternalUserDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
}
