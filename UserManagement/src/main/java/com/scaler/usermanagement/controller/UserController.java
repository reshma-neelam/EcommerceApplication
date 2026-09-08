package com.scaler.usermanagement.controller;

import com.scaler.usermanagement.dto.address.AddressRequestDTO;
import com.scaler.usermanagement.dto.address.AddressResponseDTO;
import com.scaler.usermanagement.dto.address.AddressUpdateRequestDTO;
import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.dto.user.UpdateProfileRequestDTO;
import com.scaler.usermanagement.security.AuthenticatedUser;
import com.scaler.usermanagement.service.AddressService;
import com.scaler.usermanagement.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProfileService userProfileService;
    private final AddressService addressService;

    public UserController(UserProfileService userProfileService, AddressService addressService) {
        this.userProfileService = userProfileService;
        this.addressService = addressService;
    }

    @GetMapping("/me")
    public UserResponseDTO me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userProfileService.getCurrent(principal.getUserId());
    }

    @PatchMapping("/me")
    public UserResponseDTO updateMe(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        return userProfileService.updateCurrent(principal.getUserId(), request);
    }

    @GetMapping("/me/addresses")
    public List<AddressResponseDTO> listAddresses(@AuthenticationPrincipal AuthenticatedUser principal) {
        return addressService.list(principal.getUserId());
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponseDTO createAddress(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody AddressRequestDTO request) {
        return addressService.create(principal.getUserId(), request);
    }

    @PatchMapping("/me/addresses/{addressId}")
    public AddressResponseDTO updateAddress(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID addressId, @Valid @RequestBody AddressUpdateRequestDTO request) {
        return addressService.update(principal.getUserId(), addressId, request);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID addressId) {
        addressService.delete(principal.getUserId(), addressId);
        return ResponseEntity.noContent().build();
    }
}
