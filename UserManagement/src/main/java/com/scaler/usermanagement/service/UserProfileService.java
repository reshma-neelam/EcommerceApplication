package com.scaler.usermanagement.service;

import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.dto.user.InternalUserDTO;
import com.scaler.usermanagement.dto.user.UpdateProfileRequestDTO;
import com.scaler.usermanagement.exception.NotFoundException;
import com.scaler.usermanagement.model.Role;
import com.scaler.usermanagement.model.User;
import com.scaler.usermanagement.repository.UserRepository;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getCurrent(UUID userId) {
        return toResponse(load(userId));
    }

    @Transactional
    public UserResponseDTO updateCurrent(UUID userId, UpdateProfileRequestDTO request) {
        User user = load(userId);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public InternalUserDTO getInternal(UUID userId) {
        User user = load(userId);
        return new InternalUserDTO(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getStatus().name());
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getStatus().name(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()), user.getCreatedAt());
    }
}
