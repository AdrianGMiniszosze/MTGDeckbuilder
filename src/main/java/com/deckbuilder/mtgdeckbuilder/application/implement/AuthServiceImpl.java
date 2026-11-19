package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.AuthService;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.AccountDisabledException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidCredentialsException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidRefreshTokenException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.JwtService;
import com.deckbuilder.mtgdeckbuilder.model.AuthSession;
import com.deckbuilder.mtgdeckbuilder.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credential check and token issuance. Login runs the password encoder even
 * when the user does not exist so the response time does not reveal accounts.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	/**
	 * Valid BCrypt hash used only to keep the failure path as expensive as the
	 * success path. It does not correspond to any accepted password.
	 */
	private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final UserService userService;
	private final PasswordHasher passwordHasher;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	@Override
	@Transactional
	public AuthSession login(String identifier, String rawPassword) {
		final User user = this.userService.findByUsernameOrEmail(identifier).orElse(null);
		if (user == null) {
			this.passwordHasher.matches(rawPassword, DUMMY_HASH);
			throw new InvalidCredentialsException();
		}
		if (!this.passwordHasher.matches(rawPassword, user.getHashedPassword())) {
			throw new InvalidCredentialsException();
		}
		if (!Boolean.TRUE.equals(user.getIsActive())) {
			throw new AccountDisabledException();
		}

		final User updated = this.userService.recordLogin(user.getId());
		return sessionFor(updated);
	}

	@Override
	public AuthSession refresh(String refreshToken) {
		final RefreshTokenService.Rotation rotation = this.refreshTokenService.rotate(refreshToken);
		final User user = this.userService.findById(rotation.userId())
				.orElse(null);
		if (user == null) {
			this.refreshTokenService.revokeAllForUser(rotation.userId());
			throw new InvalidRefreshTokenException();
		}
		if (!Boolean.TRUE.equals(user.getIsActive())) {
			this.refreshTokenService.revokeAllForUser(rotation.userId());
			throw new AccountDisabledException();
		}
		return new AuthSession(this.jwtService.issueAccessToken(user), rotation.refreshToken(),
				this.jwtService.accessTokenSeconds());
	}

	@Override
	public void logout(String refreshToken) {
		this.refreshTokenService.revoke(refreshToken);
	}

	private AuthSession sessionFor(User user) {
		return new AuthSession(this.jwtService.issueAccessToken(user), this.refreshTokenService.issue(user.getId()),
				this.jwtService.accessTokenSeconds());
	}
}
