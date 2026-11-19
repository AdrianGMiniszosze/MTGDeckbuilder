package com.deckbuilder.mtgdeckbuilder.contract.config;

import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Jackson Config Tests")
class JacksonConfigTest {

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JacksonConfig().userPasswordMixin());
	}

	@Test
	@DisplayName("Should never serialize the password field")
	void shouldNotSerializePassword() throws Exception {
		// Given
		final UserDTO user = UserDTO.builder().id(1).name("John Doe").username("johndoe")
				.email("john@example.com").country("USA").hashed_password("super-secret-hash").build();

		// When
		final String json = this.objectMapper.writeValueAsString(user);

		// Then
		assertThat(json).doesNotContain("hashed_password").doesNotContain("super-secret-hash");
		assertThat(json).contains("\"username\":\"johndoe\"");
	}

	@Test
	@DisplayName("Should still accept the password when deserializing")
	void shouldDeserializePassword() throws Exception {
		// Given
		final String json = """
				{"id":1,"name":"John Doe","username":"johndoe","email":"john@example.com","country":"USA",
				 "hashed_password":"super-secret-hash"}
				""";

		// When
		final UserDTO user = this.objectMapper.readValue(json, UserDTO.class);

		// Then
		assertThat(user.getHashed_password()).isEqualTo("super-secret-hash");
	}

	@Test
	void shouldConstructThePasswordMixin() {
		assertThat(new JacksonConfig.UserPasswordMixin() {
			@Override
			String getHashed_password() {
				return "hidden";
			}
		}).isNotNull();
	}
}
