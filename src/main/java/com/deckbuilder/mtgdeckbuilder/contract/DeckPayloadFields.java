package com.deckbuilder.mtgdeckbuilder.contract;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Set;

/**
 * Tracks which optional arrays of a deck payload were actually present in the
 * request body.
 *
 * <p>
 * The generated {@code DeckDTO} uses Lombok's {@code @Singular}, so an omitted
 * {@code deck_contents} or {@code commander_card_ids} arrives as an empty list,
 * indistinguishable from an explicit {@code []}. The distinction matters: an
 * omitted field preserves the stored rows, while an empty list clears them.
 * {@link DeckPayloadPresenceAdvice} records the JSON field names before the
 * body is bound.
 */
public final class DeckPayloadFields {

	public static final String DECK_CONTENTS = "deck_contents";
	public static final String COMMANDER_CARD_IDS = "commander_card_ids";
	public static final String ATTRIBUTE = DeckPayloadFields.class.getName();

	private DeckPayloadFields() {
	}

	/**
	 * Whether the current request explicitly sent the field. Outside a web
	 * request (direct controller calls in tests) the payload is assumed to be
	 * complete, so the mapped value is used as-is.
	 */
	public static boolean wasSent(String field) {
		final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return true;
		}
		final Object recorded = attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		return !(recorded instanceof Set<?> fields) || fields.contains(field);
	}
}
