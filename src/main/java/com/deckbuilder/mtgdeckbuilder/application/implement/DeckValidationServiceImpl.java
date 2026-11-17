package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CompanionRules;
import com.deckbuilder.mtgdeckbuilder.application.CommanderRules;
import com.deckbuilder.mtgdeckbuilder.application.DeckValidationService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckCommanderRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.FormatNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Evaluates deck composition and reports every problem it finds instead of
 * throwing on the first one.
 *
 * <p>
 * Copy limits are counted by {@code oracle_id}: a card is the same card across
 * printings, so four copies of two printings are eight copies. Basic lands are
 * exempt and {@code cards.copies_limit} wins over the format (0 = any number,
 * N = exact cap). {@code maybeboard} is ignored for both counts and sizes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeckValidationServiceImpl implements DeckValidationService {

	private static final int UNLIMITED_QUANTITY = 9999;
	private static final String MAIN = "main";
	private static final String SIDEBOARD = "sideboard";
	private static final String MAYBEBOARD = "maybeboard";
	private static final Set<String> COUNTED_SECTIONS = Set.of(MAIN, SIDEBOARD);

	private final DeckRepository deckRepository;
	private final CardRepository cardRepository;
	private final FormatRepository formatRepository;
	private final CardLegalityRepository cardLegalityRepository;
	private final CardInDeckRepository cardInDeckRepository;
	private final DeckCommanderRepository deckCommanderRepository;

	@Override
	@Transactional(readOnly = true)
	public DeckValidationResult evaluate(Long deckId) {
		log.debug("Evaluating deck {}", deckId);

		final DeckEntity deck = this.deckRepository.findById(deckId)
			.orElseThrow(() -> new DeckNotFoundException(deckId));

		final Long formatId = deck.getFormatId();
		if (formatId == null) {
			throw new InvalidDeckCompositionException("Deck format is not specified");
		}
		final FormatEntity format = this.formatRepository.findById(formatId)
			.orElseThrow(() -> new FormatNotFoundException(formatId));

		final List<CardInDeckEntity> entries = this.cardInDeckRepository.findByDeckId(deckId);
		final Map<Long, CardEntity> cardsById = this.cardsById(entries);
		final List<DeckIssue> issues = new ArrayList<>();
		final List<Long> commanderCardIds = format.isRequiresCommander() ? this.commanderCardIds(deckId) : List.of();

		this.checkCopyLimits(entries, cardsById, format, issues);
		if (format.isTracksLegality()) {
			this.checkLegality(entries, cardsById, format, Set.copyOf(commanderCardIds), issues);
		}
		this.checkSectionSizes(entries, format, issues);
		if (format.isRequiresCommander()) {
			this.checkCommanderRules(entries, cardsById, commanderCardIds, issues);
		}
		if (deck.getCompanionCardId() != null) {
			this.checkCompanionRules(deck, entries, cardsById, format, issues);
		}

		return this.buildResult(deck, issues);
	}

	// ---------------------------------------------------------------------
	// Copy limits
	// ---------------------------------------------------------------------

	private void checkCopyLimits(List<CardInDeckEntity> entries, Map<Long, CardEntity> cardsById,
			FormatEntity format, List<DeckIssue> issues) {
		final Map<String, List<CardInDeckEntity>> byCardIdentity = new LinkedHashMap<>();
		for (final CardInDeckEntity entry : entries) {
			if (!COUNTED_SECTIONS.contains(entry.getSection())) {
				continue;
			}
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card == null) {
				continue;
			}
			byCardIdentity.computeIfAbsent(this.cardIdentity(card), key -> new ArrayList<>()).add(entry);
		}
		if (byCardIdentity.isEmpty()) {
			return;
		}

		final Map<Long, String> legality = format.isTracksLegality()
				? this.legalityByCardId(format.getId(), cardsById.keySet())
				: Map.of();

		for (final List<CardInDeckEntity> group : byCardIdentity.values()) {
			final int total = group.stream().mapToInt(CardInDeckEntity::getQuantity).sum();
			final CopyLimit limit = this.resolveCopyLimit(group, cardsById, format, legality);
			if (limit.unlimited() || total <= limit.max()) {
				continue;
			}

			final CardInDeckEntity reference = group.get(0);
			final CardEntity card = cardsById.get(reference.getCardId());
			final boolean singleSection = group.stream().map(CardInDeckEntity::getSection).distinct().count() == 1;

			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(limit.exceededCode())
				.message(String.format("%s: %d copies exceed the limit of %d for this format.", card.getName(), total,
						limit.max()))
				.cardId(card.getId())
				.deckContentId(group.size() == 1 ? reference.getId() : null)
				.section(singleSection ? reference.getSection() : null)
				.build());
		}
	}

	private CopyLimit resolveCopyLimit(List<CardInDeckEntity> group, Map<Long, CardEntity> cardsById,
			FormatEntity format, Map<Long, String> legality) {
		final List<CardEntity> cards = group.stream()
			.map(entry -> cardsById.get(entry.getCardId()))
			.filter(Objects::nonNull)
			.toList();

		// Card text wins over the format: "any number of cards named X" (0) or
		// "up to N cards named X" (N).
		final List<Integer> explicitLimits = cards.stream()
			.map(CardEntity::getCopiesLimit)
			.filter(Objects::nonNull)
			.toList();
		if (explicitLimits.contains(0)) {
			return CopyLimit.noLimit();
		}
		if (!explicitLimits.isEmpty()) {
			return new CopyLimit(explicitLimits.stream().min(Integer::compare).orElseThrow(), false,
					DeckIssueCode.MAX_COPIES_EXCEEDED);
		}

		if (cards.stream().anyMatch(card -> Boolean.TRUE.equals(card.getIsBasicLand()))) {
			return CopyLimit.noLimit();
		}

		if (group.stream().anyMatch(entry -> "restricted".equals(legality.get(entry.getCardId())))) {
			return new CopyLimit(1, false, DeckIssueCode.MAX_COPIES_EXCEEDED);
		}

		if (format.isSingleton()) {
			return new CopyLimit(1, false, DeckIssueCode.SINGLETON_VIOLATION);
		}

		return new CopyLimit(format.getMaxCopies(), false, DeckIssueCode.MAX_COPIES_EXCEEDED);
	}

	/**
	 * Identity used to group copies: the oracle id when the import provided it,
	 * the printing otherwise. Grouping by null oracle ids would merge unrelated
	 * cards.
	 */
	private String cardIdentity(CardEntity card) {
		return card.getOracleId() != null ? "oracle:" + card.getOracleId() : "card:" + card.getId();
	}

	// ---------------------------------------------------------------------
	// Legality
	// ---------------------------------------------------------------------

	private void checkLegality(List<CardInDeckEntity> entries, Map<Long, CardEntity> cardsById, FormatEntity format,
			Set<Long> commanderIds, List<DeckIssue> issues) {
		final Map<Long, String> legality = this.legalityByCardId(format.getId(), cardsById.keySet());

		for (final CardInDeckEntity entry : entries) {
			if (MAYBEBOARD.equals(entry.getSection())) {
				continue;
			}
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card == null) {
				continue;
			}

			final boolean commander = commanderIds.contains(entry.getCardId());
			final String status = legality.get(entry.getCardId());
			if (status == null) {
				issues.add(this.cardIssue(IssueSeverity.WARNING, DeckIssueCode.LEGALITY_UNKNOWN, card, entry,
						String.format("%s has no legality data for this format; treated as unknown.", card.getName())));
			} else if ("banned".equals(status)) {
				issues.add(this.cardIssue(IssueSeverity.ERROR,
						commander ? DeckIssueCode.COMMANDER_NOT_LEGAL : DeckIssueCode.CARD_BANNED, card, entry,
						String.format(commander ? "%s is banned in this format and cannot be the commander."
								: "%s is banned in this format.", card.getName())));
			} else if ("not_legal".equals(status)) {
				issues.add(this.cardIssue(IssueSeverity.ERROR,
						commander ? DeckIssueCode.COMMANDER_NOT_LEGAL : DeckIssueCode.CARD_NOT_LEGAL, card, entry,
						String.format(commander ? "%s is not legal in this format and cannot be the commander."
								: "%s is not legal in this format.", card.getName())));
			}
		}
	}

	private Map<Long, String> legalityByCardId(Long formatId, Collection<Long> cardIds) {
		if (cardIds.isEmpty()) {
			return Map.of();
		}
		return this.cardLegalityRepository.findByFormatIdAndCardIdIn(formatId, cardIds)
			.stream()
			.collect(Collectors.toMap(CardLegalityEntity::getCardId, CardLegalityEntity::getLegalityStatus,
					(first, second) -> first));
	}

	// ---------------------------------------------------------------------
	// Section sizes
	// ---------------------------------------------------------------------

	private void checkSectionSizes(List<CardInDeckEntity> entries, FormatEntity format, List<DeckIssue> issues) {
		final int mainSize = this.sectionSize(entries, MAIN);
		final Integer maxDeckSize = format.getMaxDeckSize();
		if (maxDeckSize != null && mainSize > maxDeckSize) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.DECK_TOO_LARGE)
				.message(String.format("Main deck has %d cards; the format allows at most %d.", mainSize, maxDeckSize))
				.section(MAIN)
				.build());
		}
		if (mainSize < format.getMinDeckSize()) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.DECK_TOO_SMALL)
				.message(String.format("Main deck has %d cards; the format requires at least %d.", mainSize,
						format.getMinDeckSize()))
				.section(MAIN)
				.build());
		}

		final int sideboardSize = this.sectionSize(entries, SIDEBOARD);
		final Integer maxSideboardSize = format.getMaxSideboardSize();
		if (maxSideboardSize != null && sideboardSize > maxSideboardSize) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.SIDEBOARD_TOO_LARGE)
				.message(String.format("Sideboard has %d cards; the format allows at most %d.", sideboardSize,
						maxSideboardSize))
				.section(SIDEBOARD)
				.build());
		}
	}

	private int sectionSize(List<CardInDeckEntity> entries, String section) {
		return entries.stream()
			.filter(entry -> section.equals(entry.getSection()))
			.mapToInt(CardInDeckEntity::getQuantity)
			.sum();
	}

	// ---------------------------------------------------------------------
	// Companion rules
	// ---------------------------------------------------------------------

	private void checkCompanionRules(DeckEntity deck, List<CardInDeckEntity> entries,
			Map<Long, CardEntity> cardsById, FormatEntity format, List<DeckIssue> issues) {
		final Optional<CardEntity> companion = this.cardRepository.findById(deck.getCompanionCardId());
		if (companion.isEmpty()) {
			log.warn("Deck {} references companion card {} which does not exist", deck.getId(),
					deck.getCompanionCardId());
			return;
		}
		final String companionName = companion.get().getName();
		final String normalizedName = companionName != null ? companionName.toLowerCase() : "";
		final List<CardInDeckEntity> mainEntries = entries.stream()
			.filter(entry -> MAIN.equals(entry.getSection()))
			.toList();

		for (final CardInDeckEntity entry : mainEntries) {
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card == null) {
				continue;
			}
			final String violation = CompanionRules.validateByCompanionName(companionName, card);
			if (violation != null) {
				issues.add(this.cardIssue(IssueSeverity.ERROR, DeckIssueCode.COMPANION_RULE_VIOLATION, card, entry,
						violation));
			}
		}

		if (normalizedName.contains("umori")) {
			this.checkUmori(mainEntries, cardsById, issues);
		}
		if (normalizedName.contains("yorion")) {
			this.checkYorion(entries, format, issues);
		}
		if (normalizedName.contains("zirda")) {
			this.checkZirda(mainEntries, cardsById, issues);
		}
	}

	private void checkUmori(List<CardInDeckEntity> mainEntries, Map<Long, CardEntity> cardsById,
			List<DeckIssue> issues) {
		final Set<String> nonlandTypes = new HashSet<>();
		for (final CardInDeckEntity entry : mainEntries) {
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card == null || card.getCardType() == null || card.getCardType().contains("Land")) {
				continue;
			}
			nonlandTypes.add(card.getCardType());
		}
		if (nonlandTypes.size() > 1) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.COMPANION_RULE_VIOLATION)
				.message(String.format(
						"Companion Umori: all nonland cards must share the same card type, found %s.", nonlandTypes))
				.section(MAIN)
				.build());
		}
	}

	private void checkYorion(List<CardInDeckEntity> entries, FormatEntity format, List<DeckIssue> issues) {
		final int required = format.getMinDeckSize() + 20;
		final int mainSize = this.sectionSize(entries, MAIN);
		if (mainSize < required) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.COMPANION_RULE_VIOLATION)
				.message(String.format("Companion Yorion: main deck must have at least %d cards (%d + 20).", required,
						format.getMinDeckSize()))
				.section(MAIN)
				.build());
		}
	}

	private void checkZirda(List<CardInDeckEntity> mainEntries, Map<Long, CardEntity> cardsById,
			List<DeckIssue> issues) {
		for (final CardInDeckEntity entry : mainEntries) {
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card != null && CompanionRules.isPermanent(card) && !this.hasActivatedAbility(card)) {
				issues.add(this.cardIssue(IssueSeverity.ERROR, DeckIssueCode.COMPANION_RULE_VIOLATION, card, entry,
						"Companion Zirda: each permanent card in the main deck must have an activated ability."));
			}
		}
	}

	// ---------------------------------------------------------------------
	// Commander rules
	// ---------------------------------------------------------------------

	/**
	 * Commander rows of a deck, ordered by card id so diagnostics and responses
	 * are deterministic.
	 */
	private List<Long> commanderCardIds(Long deckId) {
		return this.deckCommanderRepository.findByDeckId(deckId)
			.stream()
			.map(DeckCommanderEntity::getCardId)
			.sorted()
			.toList();
	}

	/**
	 * A commander is a card of the deck: it must exist in {@code main} and, when
	 * the format asks for one, the deck must declare it. Pairs are checked
	 * against the partner variants and colors must fit the commanders' identity.
	 */
	private void checkCommanderRules(List<CardInDeckEntity> entries, Map<Long, CardEntity> cardsById,
			List<Long> commanderCardIds, List<DeckIssue> issues) {
		if (commanderCardIds.isEmpty()) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.MISSING_COMMANDER)
				.message("Commander format: the deck must declare at least one commander.")
				.build());
			return;
		}
		if (commanderCardIds.size() > 2) {
			issues.add(DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.TOO_MANY_COMMANDERS)
				.message(String.format("A deck can declare at most two commanders; found %d.", commanderCardIds.size()))
				.build());
			return;
		}

		final Map<Long, CardInDeckEntity> mainEntries = new LinkedHashMap<>();
		for (final CardInDeckEntity entry : entries) {
			if (MAIN.equals(entry.getSection())) {
				mainEntries.putIfAbsent(entry.getCardId(), entry);
			}
		}

		final List<CardEntity> commanders = new ArrayList<>();
		for (final Long commanderId : commanderCardIds) {
			final CardEntity card = cardsById.get(commanderId);
			final CardInDeckEntity entry = mainEntries.get(commanderId);
			if (card == null || entry == null) {
				issues.add(DeckIssue.builder()
					.severity(IssueSeverity.ERROR)
					.code(DeckIssueCode.INVALID_COMMANDER)
					.message(card == null ? String.format("Commander card %d does not exist in this deck.", commanderId)
							: String.format("%s must be in the main deck to be the commander.", card.getName()))
					.cardId(commanderId)
					.deckContentId(entry != null ? entry.getId() : null)
					.section(MAIN)
					.build());
				continue;
			}
			commanders.add(card);
		}

		if (commanders.size() == 1) {
			final CardEntity commander = commanders.getFirst();
			this.reportViolation(DeckIssueCode.INVALID_COMMANDER, commander, mainEntries.get(commander.getId()),
					CommanderRules.validateAlone(commander), issues);
		} else if (commanders.size() == 2) {
			final CardEntity first = commanders.get(0);
			final CardEntity second = commanders.get(1);
			final String pairViolation = CommanderRules.validatePair(first, second);
			if (pairViolation != null) {
				issues.add(DeckIssue.builder()
					.severity(IssueSeverity.ERROR)
					.code(DeckIssueCode.INVALID_COMMANDER_PAIR)
					.message(pairViolation)
					.cardId(first.getId())
					.deckContentId(mainEntries.get(first.getId()).getId())
					.section(MAIN)
					.build());
			} else {
				for (final CardEntity commander : commanders) {
					if (CommanderRules.isCommanderOnItsOwn(commander) || CommanderRules.isBackground(commander)) {
						continue;
					}
					this.reportViolation(DeckIssueCode.INVALID_COMMANDER, commander,
							mainEntries.get(commander.getId()), CommanderRules.validateAlone(commander), issues);
				}
			}
		}

		if (!commanders.isEmpty()) {
			this.checkColorIdentity(entries, cardsById, commanders, issues);
		}
	}

	/**
	 * Every card in main or sideboard must fit the union of the commanders'
	 * color identity; {@code maybeboard} is not part of the playable deck.
	 */
	private void checkColorIdentity(List<CardInDeckEntity> entries, Map<Long, CardEntity> cardsById,
			List<CardEntity> commanders, List<DeckIssue> issues) {
		final Set<String> commanderIdentity = commanders.stream()
			.flatMap(commander -> CommanderRules.colorIdentity(commander).stream())
			.collect(Collectors.toCollection(TreeSet::new));

		for (final CardInDeckEntity entry : entries) {
			if (!COUNTED_SECTIONS.contains(entry.getSection())) {
				continue;
			}
			final CardEntity card = cardsById.get(entry.getCardId());
			if (card == null) {
				continue;
			}
			final Set<String> identity = CommanderRules.colorIdentity(card);
			if (commanderIdentity.containsAll(identity)) {
				continue;
			}
			issues.add(this.cardIssue(IssueSeverity.ERROR, DeckIssueCode.COLOR_IDENTITY_VIOLATION, card, entry,
					String.format("%s has color identity %s, outside the commander identity %s.", card.getName(),
							String.join(",", identity), String.join(",", commanderIdentity))));
		}
	}

	private void reportViolation(DeckIssueCode code, CardEntity card, CardInDeckEntity entry, String message,
			List<DeckIssue> issues) {
		if (message != null) {
			issues.add(this.cardIssue(IssueSeverity.ERROR, code, card, entry, message));
		}
	}

	// ---------------------------------------------------------------------
	// Result
	// ---------------------------------------------------------------------

	private DeckValidationResult buildResult(DeckEntity deck, List<DeckIssue> issues) {
		final boolean hasError = issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.ERROR);
		final boolean hasUnknownLegality = issues.stream()
			.anyMatch(issue -> issue.getCode() == DeckIssueCode.LEGALITY_UNKNOWN);

		final ValidationStatus validationStatus = hasError ? ValidationStatus.ILLEGAL
				: hasUnknownLegality ? ValidationStatus.UNKNOWN : ValidationStatus.LEGAL;

		return DeckValidationResult.builder()
			.deckId(deck.getId())
			.status(DeckStatus.fromValue(deck.getStatus()))
			.validationStatus(validationStatus)
			.issues(List.copyOf(issues))
			.build();
	}

	private DeckIssue cardIssue(IssueSeverity severity, DeckIssueCode code, CardEntity card, CardInDeckEntity entry,
			String message) {
		return DeckIssue.builder()
			.severity(severity)
			.code(code)
			.message(message)
			.cardId(card.getId())
			.deckContentId(entry.getId())
			.section(entry.getSection())
			.build();
	}

	// ---------------------------------------------------------------------
	// Helpers kept for callers that only need a single answer
	// ---------------------------------------------------------------------

	@Override
	public void validateCardRemoval(Long deckId, Long cardId, Integer quantity, String section) {
		log.debug("Validating card removal: deckId={}, cardId={}, quantity={}, section={}", deckId, cardId, quantity,
				section);

		if (quantity == null || quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}

		final Optional<CardInDeckEntity> existingCard = this.cardInDeckRepository
			.findByDeckIdAndCardIdAndSection(deckId, cardId, section);

		if (existingCard.isEmpty()) {
			throw new InvalidDeckCompositionException("Card not found in deck section");
		}

		final int currentQuantity = existingCard.get().getQuantity();
		if (quantity > currentQuantity) {
			throw new InvalidDeckCompositionException(
					String.format("Cannot remove %d cards, only %d available", quantity, currentQuantity));
		}
	}

	@Override
	public int getMaxAllowedQuantity(Long cardId, Long formatId) {
		final CardEntity card = this.cardRepository.findById(cardId)
			.orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));

		final Optional<CardLegalityEntity> cardLegality = this.cardLegalityRepository
			.findByCardIdAndFormatId(cardId, formatId);

		return this.getMaxAllowedQuantity(card, cardLegality.orElse(null), formatId);
	}

	private int getMaxAllowedQuantity(CardEntity card, CardLegalityEntity cardLegality, Long formatId) {
		final Integer copiesLimit = card.getCopiesLimit();
		if (copiesLimit != null) {
			return copiesLimit == 0 ? UNLIMITED_QUANTITY : copiesLimit;
		}

		if (Boolean.TRUE.equals(card.getIsBasicLand())) {
			return UNLIMITED_QUANTITY;
		}

		if (cardLegality != null && "restricted".equals(cardLegality.getLegalityStatus())) {
			return 1;
		}

		final FormatEntity format = this.formatRepository.findById(formatId)
			.orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

		if (format.isSingleton()) {
			return 1;
		}

		return format.getMaxCopies();
	}

	@Override
	public int getMaxDeckSize(Long formatId, String section) {
		final FormatEntity format = this.formatRepository.findById(formatId)
			.orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

		final Integer limit = SIDEBOARD.equals(section) ? format.getMaxSideboardSize() : format.getMaxDeckSize();
		return limit != null ? limit : Integer.MAX_VALUE;
	}

	private Map<Long, CardEntity> cardsById(List<CardInDeckEntity> entries) {
		final Set<Long> cardIds = entries.stream().map(CardInDeckEntity::getCardId).collect(Collectors.toSet());
		if (cardIds.isEmpty()) {
			return Map.of();
		}
		return this.cardRepository.findAllById(cardIds)
			.stream()
			.collect(Collectors.toMap(CardEntity::getId, card -> card));
	}

	// ---------------------------------------------------------------------
	// Zirda heuristics
	// ---------------------------------------------------------------------

	/**
	 * Heuristic to detect an activated ability: looks for a colon with a cost-like
	 * left side.
	 */
	private boolean hasActivatedAbility(CardEntity card) {
		final String text = card.getCardText();
		if (text == null) {
			return false;
		}
		final String normalized = text.toLowerCase().replace("\n", " ");
		if (!normalized.contains(":")) {
			return false;
		}
		for (final String clause : normalized.split("\\.")) {
			final String trimmed = clause.trim();
			final int idx = trimmed.indexOf(":");
			if (idx <= 0) {
				continue;
			}
			final String left = trimmed.substring(0, idx).trim();
			if (left.contains("{") || left.contains("tap") || left.matches(".*[0-9].*") || left.startsWith("sacrifice")
					|| left.startsWith("discard") || left.startsWith("exile") || left.startsWith("return")
					|| left.startsWith("pay") || left.startsWith("remove")) {
				// "untap" is already matched by contains("tap"), so it needs no
				// separate case.
				return true;
			}
		}
		return false;
	}

	private record CopyLimit(int max, boolean unlimited, DeckIssueCode exceededCode) {
		private static CopyLimit noLimit() {
			return new CopyLimit(Integer.MAX_VALUE, true, DeckIssueCode.MAX_COPIES_EXCEEDED);
		}
	}
}
