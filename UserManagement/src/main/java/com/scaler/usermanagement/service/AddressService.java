package com.scaler.usermanagement.service;

import com.scaler.usermanagement.dto.address.AddressRequestDTO;
import com.scaler.usermanagement.dto.address.AddressResponseDTO;
import com.scaler.usermanagement.dto.address.AddressUpdateRequestDTO;
import com.scaler.usermanagement.enums.AddressType;
import com.scaler.usermanagement.exception.NotFoundException;
import com.scaler.usermanagement.model.UserAddress;
import com.scaler.usermanagement.repository.UserAddressRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final UserAddressRepository addressRepository;

    public AddressService(UserAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponseDTO> list(UUID userId) {
        return addressRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressResponseDTO create(UUID userId, AddressRequestDTO request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setAddressType(AddressType.valueOf(request.getAddressType().toUpperCase()));
        address.setRecipientName(request.getRecipientName());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setStateRegion(request.getStateRegion());
        address.setPostalCode(request.getPostalCode());
        address.setCountryCode(request.getCountryCode().toUpperCase());
        address.setPhone(request.getPhone());
        if (request.isDefault()) {
            clearDefaults(userId, address.getAddressType());
            address.setDefault(true);
        }
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponseDTO update(UUID userId, UUID addressId, AddressUpdateRequestDTO request) {
        UserAddress address = load(userId, addressId);
        if (request.getRecipientName() != null) {
            address.setRecipientName(request.getRecipientName());
        }
        if (request.getLine1() != null) {
            address.setLine1(request.getLine1());
        }
        if (request.getLine2() != null) {
            address.setLine2(request.getLine2());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getStateRegion() != null) {
            address.setStateRegion(request.getStateRegion());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaults(userId, address.getAddressType());
            address.setDefault(true);
        }
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        UserAddress address = load(userId, addressId);
        addressRepository.delete(address);
    }

    private void clearDefaults(UUID userId, AddressType type) {
        addressRepository.findByUserId(userId).stream()
                .filter(a -> a.getAddressType() == type && a.isDefault())
                .forEach(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }

    private UserAddress load(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
    }

    private AddressResponseDTO toResponse(UserAddress a) {
        return new AddressResponseDTO(a.getId(), a.getAddressType().name(), a.getRecipientName(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getStateRegion(), a.getPostalCode(),
                a.getCountryCode(), a.getPhone(), a.isDefault());
    }
}
