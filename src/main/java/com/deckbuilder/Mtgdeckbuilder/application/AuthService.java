package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.contract.dto.AuthResponseDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.LoginRequestDTO;
import com.deckbuilder.mtgdeckbuilder.contract.dto.RegisterRequestDTO;

/**
 * Authentication Service Interface
 */
public interface AuthService {
    /**
     * Authenticate user and generate JWT token
     */
    AuthResponseDTO login(LoginRequestDTO loginRequest);

    /**
     * Register a new user
     */
    AuthResponseDTO register(RegisterRequestDTO registerRequest);
}
