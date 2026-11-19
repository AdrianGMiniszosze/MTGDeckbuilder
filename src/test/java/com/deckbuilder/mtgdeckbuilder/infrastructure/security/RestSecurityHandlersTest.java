package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("REST Security Handlers Tests")
class RestSecurityHandlersTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	@DisplayName("Should render a JSON 401")
	void shouldRenderUnauthorized() throws Exception {
		final MockHttpServletResponse response = new MockHttpServletResponse();
		final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/decks/1");

		new RestAuthenticationEntryPoint(this.objectMapper).commence(request, response,
				new BadCredentialsException("missing token"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).contains("application/json");
		assertThat(response.getContentAsString()).contains("\"error\":\"UNAUTHORIZED\"")
			.contains("\"path\":\"/decks/1\"");
	}

	@Test
	@DisplayName("Should render a JSON 403")
	void shouldRenderForbidden() throws Exception {
		final MockHttpServletResponse response = new MockHttpServletResponse();
		final MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/decks/1");

		new RestAccessDeniedHandler(this.objectMapper).handle(request, response,
				new AccessDeniedException("not allowed"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).contains("application/json");
		assertThat(response.getContentAsString()).contains("\"error\":\"FORBIDDEN\"")
			.contains("\"path\":\"/decks/1\"");
	}
}
