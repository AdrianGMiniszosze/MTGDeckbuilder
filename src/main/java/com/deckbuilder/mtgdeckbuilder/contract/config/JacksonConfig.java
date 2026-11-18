package com.deckbuilder.mtgdeckbuilder.contract.config;

import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI generator ignores {@code writeOnly: true}, so the password field
 * is hidden from responses with a Jackson mixin instead of editing generated
 * code. Deserialization still accepts it.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public Module userPasswordMixin() {
		return new SimpleModule().setMixInAnnotation(UserDTO.class, UserPasswordMixin.class);
	}

	abstract static class UserPasswordMixin {
		@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
		abstract String getHashed_password();
	}
}
