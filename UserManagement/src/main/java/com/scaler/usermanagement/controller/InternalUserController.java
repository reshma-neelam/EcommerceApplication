package com.scaler.usermanagement.controller;

import com.scaler.usermanagement.dto.user.InternalUserDTO;
import com.scaler.usermanagement.service.UserProfileService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/users")
public class InternalUserController {

    private final UserProfileService userProfileService;

    public InternalUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}")
    public InternalUserDTO getUser(@PathVariable UUID userId) {
        return userProfileService.getInternal(userId);
    }
}
