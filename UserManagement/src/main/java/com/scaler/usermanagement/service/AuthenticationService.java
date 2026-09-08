package com.scaler.usermanagement.service;

import com.scaler.usermanagement.config.JwtProperties;
import com.scaler.usermanagement.config.SecuritySettingsProperties;
import com.scaler.usermanagement.dto.auth.AuthTokensDTO;
import com.scaler.usermanagement.dto.auth.LoginRequestDTO;
import com.scaler.usermanagement.dto.auth.LogoutRequestDTO;
import com.scaler.usermanagement.dto.auth.RefreshRequestDTO;
import com.scaler.usermanagement.dto.auth.SignupRequestDTO;
import com.scaler.usermanagement.dto.auth.UserResponseDTO;
import com.scaler.usermanagement.enums.RoleName;
import com.scaler.usermanagement.exception.ConflictException;
import com.scaler.usermanagement.exception.UnauthorizedException;
import com.scaler.usermanagement.model.Role;
import com.scaler.usermanagement.model.User;
import com.scaler.usermanagement.model.UserCredential;
import com.scaler.usermanagement.model.UserSession;
import com.scaler.usermanagement.repository.RoleRepository;
import com.scaler.usermanagement.repository.UserCredentialRepository;
import com.scaler.usermanagement.repository.UserRepository;
import com.scaler.usermanagement.repository.UserSessionRepository;
import com.scaler.usermanagement.security.RefreshTokenSupport;
import com.scaler.usermanagement.security.TokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserSessionRepository sessionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenSupport refreshTokenSupport;
    private final JwtProperties jwtProperties;
    private final SecuritySettingsProperties settings;

    public AuthenticationService(UserRepository userRepository, UserCredentialRepository credentialRepository,
            UserSessionRepository sessionRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            TokenService tokenService, RefreshTokenSupport refreshTokenSupport, JwtProperties jwtProperties,
            SecuritySettingsProperties settings) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.sessionRepository = sessionRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenSupport = refreshTokenSupport;
        this.jwtProperties = jwtProperties;
        this.settings = settings;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    @Transactional
    public UserResponseDTO signup(SignupRequestDTO request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_EXISTS", "Email already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        Role customer = roleRepository.findByName(RoleName.CUSTOMER.name())
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role missing"));
        user.getRoles().add(customer);
        userRepository.save(user);

        UserCredential credential = new UserCredential();
        credential.setUserId(user.getId());
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setPasswordAlgorithm("BCRYPT");
        credentialRepository.save(credential);

        return toUserResponse(user);
    }

    @Transactional
    public AuthTokensDTO login(LoginRequestDTO request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));
        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokensDTO refresh(RefreshRequestDTO request) {
        String hash = refreshTokenSupport.hash(request.getRefreshToken());
        UserSession session = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        Instant now = Instant.now();
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token expired or revoked");
        }
        session.setRevokedAt(now);
        sessionRepository.save(session);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequestDTO request) {
        String hash = refreshTokenSupport.hash(request.getRefreshToken());
        sessionRepository.findByRefreshTokenHash(hash).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(Instant.now());
                sessionRepository.save(session);
            }
        });
    }

    private AuthTokensDTO issueTokens(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        String accessToken = tokenService.createAccessToken(user.getId(), roles);

        String refreshToken = refreshTokenSupport.generateToken();
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setRefreshTokenHash(refreshTokenSupport.hash(refreshToken));
        session.setExpiresAt(Instant.now().plus(settings.getRefresh().getTtlDays(), ChronoUnit.DAYS));
        sessionRepository.save(session);

        return new AuthTokensDTO(accessToken, refreshToken, "Bearer", jwtProperties.getAccessTokenTtlSeconds());
    }

    UserResponseDTO toUserResponse(User user) {
        return new UserResponseDTO(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getStatus().name(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.getCreatedAt());
    }
}
