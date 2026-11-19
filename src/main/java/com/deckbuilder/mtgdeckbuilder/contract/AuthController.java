package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.AuthApi;
import com.deckbuilder.apigenerator.openapi.api.model.LoginRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.EmailRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.PasswordResetConfirmRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.RefreshRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.RegisterRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.TokenResponseDTO;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.apigenerator.openapi.api.model.VerifyEmailRequestDTO;
import com.deckbuilder.mtgdeckbuilder.application.AuthService;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.application.implement.AccountLifecycleService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UserNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CurrentActorProvider;
import com.deckbuilder.mtgdeckbuilder.model.AuthSession;
import com.deckbuilder.mtgdeckbuilder.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
	private final AuthService authService;
	private final UserService userService;
	private final AccountLifecycleService accountLifecycleService;
	private final UserMapper userMapper;
	private final CurrentActorProvider currentActorProvider;

	@Override
	public ResponseEntity<UserDTO> registerUser(@Valid RegisterRequestDTO registerRequestDTO) {
		final User user = User.builder().name(registerRequestDTO.getName())
				.username(registerRequestDTO.getUsername()).email(registerRequestDTO.getEmail())
				.country(registerRequestDTO.getCountry()).hashedPassword(registerRequestDTO.getPassword()).build();
		final User created = this.accountLifecycleService.register(user);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.userMapper.toUserDTO(created));
	}

	@Override
	public ResponseEntity<TokenResponseDTO> loginUser(@Valid LoginRequestDTO loginRequestDTO) {
		return ResponseEntity.ok(toResponse(
				this.authService.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword())));
	}

	@Override
	public ResponseEntity<TokenResponseDTO> refreshTokens(@Valid RefreshRequestDTO refreshRequestDTO) {
		return ResponseEntity.ok(toResponse(this.authService.refresh(refreshRequestDTO.getRefresh_token())));
	}

	@Override
	public ResponseEntity<Void> logoutUser(@Valid RefreshRequestDTO refreshRequestDTO) {
		this.authService.logout(refreshRequestDTO.getRefresh_token());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> verifyEmail(@Valid VerifyEmailRequestDTO verifyEmailRequestDTO) {
		this.accountLifecycleService.verifyEmail(verifyEmailRequestDTO.getVerification_token());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> resendVerificationEmail(@Valid EmailRequestDTO emailRequestDTO) {
		this.accountLifecycleService.resendVerification(emailRequestDTO.getEmail());
		return ResponseEntity.accepted().build();
	}

	@Override
	public ResponseEntity<Void> requestPasswordReset(@Valid EmailRequestDTO emailRequestDTO) {
		this.accountLifecycleService.requestPasswordReset(emailRequestDTO.getEmail());
		return ResponseEntity.accepted().build();
	}

	@Override
	public ResponseEntity<Void> resetPassword(@Valid PasswordResetConfirmRequestDTO passwordResetConfirmRequestDTO) {
		this.accountLifecycleService.resetPassword(passwordResetConfirmRequestDTO.getReset_token(),
				passwordResetConfirmRequestDTO.getNew_password());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<UserDTO> getCurrentUser() {
		final var actor = this.currentActorProvider.requireActor();
		final User user = this.userService.findById(actor.userId())
				.orElseThrow(() -> new UserNotFoundException(actor.userId()));
		return ResponseEntity.ok(this.userMapper.toUserDTO(user));
	}

	private static TokenResponseDTO toResponse(AuthSession session) {
		return TokenResponseDTO.builder().access_token(session.accessToken()).refresh_token(session.refreshToken())
				.token_type("Bearer").expires_in((int) session.expiresIn()).build();
	}
}
