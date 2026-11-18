package com.deckbuilder.mtgdeckbuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@DisplayName("Application Entry Point Tests")
class MtgdeckbuilderApplicationTest {

	@Test
	@DisplayName("Should delegate startup to Spring Boot")
	void shouldDelegateToSpringApplication() {
		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			// When
			MtgdeckbuilderApplication.main(new String[] { "--server.port=0" });

			// Then
			spring.verify(() -> SpringApplication.run(eq(MtgdeckbuilderApplication.class), any(String[].class)));
		}
	}
}
