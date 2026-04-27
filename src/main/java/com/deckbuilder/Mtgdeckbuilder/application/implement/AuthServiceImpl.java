package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.AuthService;
import com.deckbuilder.mtgdeckbuilder.contract.dto.AuthResponseDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.LoginRequestDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.RegisterRequestDTO;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CustomUserDetails;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service Implementation
 */
@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(userDetails);

            log.info("User successfully logged in: {}", userDetails.getUsername());

            return new AuthResponseDTO(
                    token,
                    jwtTokenProvider.getTokenType(),
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getUser().getRole().name()
            );
        } catch (Exception e) {
            log.error("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            throw new RuntimeException("Invalid username or password", e);
        }
    }

    @Override
    public AuthResponseDTO register(RegisterRequestDTO registerRequest) {
        // Check if user already exists
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        UserEntity user = new UserEntity();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());
        user.setCountry(registerRequest.getCountry());
        user.setHashedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(UserEntity.UserRole.ROLE_USER);

        UserEntity savedUser = userRepository.save(user);

        log.info("New user registered: {}", savedUser.getUsername());

        // Generate JWT token for new user
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtTokenProvider.generateToken(userDetails);

        return new AuthResponseDTO(
                token,
                jwtTokenProvider.getTokenType(),
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }
}
