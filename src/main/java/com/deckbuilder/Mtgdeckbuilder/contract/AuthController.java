package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.mtgdeckbuilder.application.AuthService;
import com.deckbuilder.mtgdeckbuilder.contract.dto.AuthResponseDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.LoginRequestDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.RegisterRequestDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller
 * Handles user login and registration
 */
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * User login endpoint
     * POST /api/v1/auth/login
     *
     * @param loginRequest Login credentials (username, password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * User registration endpoint
     * POST /api/v1/auth/register
     *
     * @param registerRequest User registration data
     * @return JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody RegisterRequestDTO registerRequest) {
        AuthResponseDTO response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
