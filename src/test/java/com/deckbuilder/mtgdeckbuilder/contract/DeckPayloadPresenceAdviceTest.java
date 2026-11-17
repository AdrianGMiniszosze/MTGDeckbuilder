package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Deck payload presence")
class DeckPayloadPresenceAdviceTest {

	private final DeckPayloadPresenceAdvice advice = new DeckPayloadPresenceAdvice(new ObjectMapper());

	@BeforeEach
	@AfterEach
	void clearRequest() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	@DisplayName("Should only inspect deck bodies")
	void shouldOnlyInspectDeckBodies() throws Exception {
		final Class<? extends HttpMessageConverter<?>> converter = MappingJackson2HttpMessageConverter.class;

		assertThat(this.advice.supports(parameter(DeckDTO.class), DeckDTO.class, converter)).isTrue();
		assertThat(this.advice.supports(parameter(String.class), String.class, converter)).isFalse();
	}

	@Test
	@DisplayName("Should record the JSON fields and replay the body")
	void shouldRecordFieldsAndReplayBody() throws Exception {
		final MockHttpServletRequest request = this.request();
		final byte[] body = "{\"deck_name\":\"D\",\"commander_card_ids\":[]}".getBytes(StandardCharsets.UTF_8);

		final HttpInputMessage cached = this.advice.beforeBodyRead(new MockHttpInputMessage(body),
				parameter(DeckDTO.class), DeckDTO.class, MappingJackson2HttpMessageConverter.class);

		assertThat(request.getAttribute(DeckPayloadFields.ATTRIBUTE))
			.isEqualTo(Set.of("deck_name", "commander_card_ids"));
		assertThat(cached.getBody().readAllBytes()).isEqualTo(body);
	}

	@Test
	@DisplayName("Should tolerate empty, non-object and malformed bodies")
	void shouldTolerateOddBodies() throws Exception {
		final MockHttpServletRequest request = this.request();

		this.advice.beforeBodyRead(new MockHttpInputMessage(new byte[0]), parameter(DeckDTO.class), DeckDTO.class,
				MappingJackson2HttpMessageConverter.class);
		assertThat(request.getAttribute(DeckPayloadFields.ATTRIBUTE)).isEqualTo(Set.of());

		this.advice.beforeBodyRead(new MockHttpInputMessage("[]".getBytes(StandardCharsets.UTF_8)),
				parameter(DeckDTO.class), DeckDTO.class, MappingJackson2HttpMessageConverter.class);
		assertThat(request.getAttribute(DeckPayloadFields.ATTRIBUTE)).isEqualTo(Set.of());

		this.advice.beforeBodyRead(new MockHttpInputMessage("{".getBytes(StandardCharsets.UTF_8)),
				parameter(DeckDTO.class), DeckDTO.class, MappingJackson2HttpMessageConverter.class);
		assertThat(request.getAttribute(DeckPayloadFields.ATTRIBUTE)).isEqualTo(Set.of());
	}

	@Test
	@DisplayName("Should replay the body outside a web request")
	void shouldReplayBodyOutsideRequest() throws Exception {
		final byte[] body = "{\"deck_name\":\"D\"}".getBytes(StandardCharsets.UTF_8);

		final HttpInputMessage cached = this.advice.beforeBodyRead(new MockHttpInputMessage(body),
				parameter(DeckDTO.class), DeckDTO.class, MappingJackson2HttpMessageConverter.class);

		assertThat(cached.getBody().readAllBytes()).isEqualTo(body);
		assertThat(DeckPayloadFields.wasSent(DeckPayloadFields.DECK_CONTENTS)).isTrue();
	}

	@Test
	@DisplayName("Should distinguish absent fields from empty arrays")
	void shouldDistinguishAbsentFields() {
		this.request().setAttribute(DeckPayloadFields.ATTRIBUTE, Set.of("deck_name"));

		assertThat(DeckPayloadFields.wasSent("deck_name")).isTrue();
		assertThat(DeckPayloadFields.wasSent(DeckPayloadFields.DECK_CONTENTS)).isFalse();
		assertThat(DeckPayloadFields.wasSent(DeckPayloadFields.COMMANDER_CARD_IDS)).isFalse();
	}

	@Test
	@DisplayName("Should assume a complete payload when nothing was recorded")
	void shouldAssumeCompletePayloadWithoutRecord() {
		this.request();

		assertThat(DeckPayloadFields.wasSent(DeckPayloadFields.DECK_CONTENTS)).isTrue();
	}

	private MockHttpServletRequest request() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		return request;
	}

	private static MethodParameter parameter(Class<?> type) throws NoSuchMethodException {
		return new MethodParameter(DeckPayloadPresenceAdviceTest.class.getDeclaredMethod("sample", type), 0);
	}

	@SuppressWarnings("unused")
	private void sample(DeckDTO deckDTO) {
	}

	@SuppressWarnings("unused")
	private void sample(String value) {
	}
}
