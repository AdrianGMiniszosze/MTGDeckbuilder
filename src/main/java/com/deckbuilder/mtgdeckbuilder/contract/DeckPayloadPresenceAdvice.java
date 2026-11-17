package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Records the JSON fields of a deck request before Jackson binds them to the
 * generated DTO, whose {@code @Singular} arrays cannot tell "absent" from
 * "empty".
 */
@ControllerAdvice
public class DeckPayloadPresenceAdvice extends RequestBodyAdviceAdapter {

	private final ObjectMapper objectMapper;

	public DeckPayloadPresenceAdvice(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return DeckDTO.class.equals(methodParameter.getParameterType());
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
		final byte[] body = inputMessage.getBody().readAllBytes();
		final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes != null) {
			attributes.setAttribute(DeckPayloadFields.ATTRIBUTE, this.fieldNames(body),
					RequestAttributes.SCOPE_REQUEST);
		}
		return new CachedBodyHttpInputMessage(inputMessage.getHeaders(), body);
	}

	private Set<String> fieldNames(byte[] body) {
		if (body.length == 0) {
			return Set.of();
		}		try {
			final JsonNode node = this.objectMapper.readTree(body);
			if (!node.isObject()) {
				return Set.of();
			}
			final Set<String> names = new HashSet<>();
			node.fieldNames().forEachRemaining(names::add);
			return Set.copyOf(names);
		} catch (IOException exception) {
			// Binding will report the malformed body with its own 400.
			return Set.of();
		}
	}

	private static final class CachedBodyHttpInputMessage implements HttpInputMessage {

		private final HttpHeaders headers;
		private final byte[] body;

		private CachedBodyHttpInputMessage(HttpHeaders headers, byte[] body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.body);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}
	}
}
