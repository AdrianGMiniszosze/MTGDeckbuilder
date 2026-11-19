package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security. Public reads stay anonymous; every write and every
 * personal resource needs a bearer token. Ownership rules that depend on the
 * loaded row live in {@code DeckAccessPolicy}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtService jwtService;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	@Bean
	public JwtDecoder jwtDecoder() {
		// Fail fast at startup when the web profile has no usable secret.
		return this.jwtService.decoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsProperties corsProperties) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh",
								"/auth/logout", "/auth/verify-email", "/auth/verify-email/resend",
								"/auth/password-reset/request", "/auth/password-reset")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/cards/**", "/sets/**", "/formats/**", "/tags/**")
						.permitAll().requestMatchers(HttpMethod.GET, "/decks", "/decks/*", "/decks/*/validation")
						.permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/actuator/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(this.authenticationEntryPoint)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(this.authenticationEntryPoint)
						.accessDeniedHandler(this.accessDeniedHandler));

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		final CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.getAllowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * Maps the {@code role} claim to a Spring Security authority so
	 * {@code hasRole('ADMIN')} works for real access tokens. The default
	 * converter only reads the OAuth2 {@code scope}/{@code scp} claims.
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			final String role = jwt.getClaimAsString("role");
			return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
		});
		return converter;
	}
}
