package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UnauthenticatedException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Current Actor Provider Tests")
class CurrentActorProviderTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CurrentActorProvider provider;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("Should return null without authentication")
	void shouldReturnNullWithoutAuthentication() {
		assertThat(this.provider.currentActorOrNull()).isNull();
		assertThat(Actor.anonymous()).isNull();
	}

	@Test
	@DisplayName("Should return null for an anonymous authentication")
	void shouldReturnNullForAnonymous() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

		assertThat(this.provider.currentActorOrNull()).isNull();
	}

	@Test
	@DisplayName("Should return null for an unauthenticated token")
	void shouldReturnNullForUnauthenticatedToken() {
		final UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken
				.unauthenticated("johndoe", "secret");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		assertThat(this.provider.currentActorOrNull()).isNull();
	}

	@Test
	@DisplayName("Should read numeric uid and admin role from a JWT")
	void shouldReadNumericUidFromJwt() {
		SecurityContextHolder.getContext().setAuthentication(jwt("johndoe", 5L, "ADMIN"));

		final Actor actor = this.provider.currentActorOrNull();

		assertThat(actor).isEqualTo(new Actor(5L, "johndoe", true));
	}

	@Test
	@DisplayName("Should read a string uid from a JWT")
	void shouldReadStringUidFromJwt() {
		SecurityContextHolder.getContext().setAuthentication(jwt("janedoe", "9", "USER"));

		final Actor actor = this.provider.currentActorOrNull();

		assertThat(actor).isEqualTo(new Actor(9L, "janedoe", false));
	}

	@Test
	@DisplayName("Should fall back to the database when the JWT has no uid")
	void shouldFallBackWithoutUid() {
		final UserEntity entity = user(3L, "janedoe", "ADMIN");
		when(this.userRepository.findByUsername("janedoe")).thenReturn(Optional.of(entity));
		SecurityContextHolder.getContext().setAuthentication(jwt("janedoe", null, null));

		assertThat(this.provider.currentActorOrNull()).isEqualTo(new Actor(3L, "janedoe", true));
	}

	@Test
	@DisplayName("Should ignore a uid claim that is not a number or string")
	void shouldIgnoreInvalidUidClaim() {
		when(this.userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
		SecurityContextHolder.getContext().setAuthentication(jwt("johndoe", true, null));

		assertThat(this.provider.currentActorOrNull()).isNull();
	}

	@Test
	@DisplayName("Should ignore a blank string uid")
	void shouldIgnoreBlankUidClaim() {
		when(this.userRepository.findByUsername("johndoe")).thenReturn(Optional.empty());
		SecurityContextHolder.getContext().setAuthentication(jwt("johndoe", "  ", null));

		assertThat(this.provider.currentActorOrNull()).isNull();
	}

	@Test
	@DisplayName("Should resolve a non-JWT authentication through the repository")
	void shouldResolveNonJwtAuthentication() {
		final UserEntity entity = user(4L, "janedoe", "USER");
		when(this.userRepository.findByUsername("janedoe")).thenReturn(Optional.of(entity));
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("janedoe", "n/a",
					AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(this.provider.currentActorOrNull()).isEqualTo(new Actor(4L, "janedoe", false));
	}

	@Test
	@DisplayName("Should return null when a non-JWT user does not exist")
	void shouldReturnNullForUnknownNonJwtUser() {
		when(this.userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("ghost", "n/a",
					AuthorityUtils.createAuthorityList("ROLE_USER")));

		assertThat(this.provider.currentActorOrNull()).isNull();
	}

	@Test
	@DisplayName("Should require an actor")
	void shouldRequireActor() {
		assertThatThrownBy(this.provider::requireActor).isInstanceOf(UnauthenticatedException.class);

		SecurityContextHolder.getContext().setAuthentication(jwt("johndoe", 1L, "USER"));
		assertThat(this.provider.requireActor()).isEqualTo(new Actor(1L, "johndoe", false));
	}

	private static JwtAuthenticationToken jwt(String subject, Object uid, String role) {
		final Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "HS256").subject(subject);
		if (uid != null) {
			builder.claim("uid", uid);
		}
		if (role != null) {
			builder.claim("role", role);
		}
		return new JwtAuthenticationToken(builder.build(), AuthorityUtils.NO_AUTHORITIES, subject);
	}

	private static UserEntity user(Long id, String username, String role) {
		final UserEntity entity = new UserEntity();
		entity.setId(id);
		entity.setUsername(username);
		entity.setRole(role);
		return entity;
	}
}
