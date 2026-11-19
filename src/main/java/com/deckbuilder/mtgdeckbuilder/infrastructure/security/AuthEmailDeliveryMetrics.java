package com.deckbuilder.mtgdeckbuilder.infrastructure.security;

import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthEmailDeliveryMetrics {

	private final MeterRegistry meterRegistry;

	public void deliveryFailed(AccountActionPurpose purpose) {
		final String action = switch (purpose) {
			case EMAIL_VERIFICATION -> "email_verification";
			case PASSWORD_RESET -> "password_reset";
		};
		this.meterRegistry.counter("auth.email.delivery.failure", "purpose", action).increment();
	}
}
