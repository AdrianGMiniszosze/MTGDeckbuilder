package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthRateLimitClockConfiguration {

	@Bean
	public Clock authRateLimitClock() {
		return Clock.systemUTC();
	}
}
