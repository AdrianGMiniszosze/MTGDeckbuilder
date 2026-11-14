package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.Format;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing MTG formats and format-related operations.
 */
public interface FormatService {

	/**
	 * Retrieves all formats.
	 *
	 * @return list of all formats
	 */
	List<Format> getAll();

	/**
	 * Finds a format by its ID.
	 *
	 * @param id
	 *            the format ID
	 * @return optional containing the format if found
	 */
	Optional<Format> findById(Long id);

	/**
	 * Finds a format by its name.
	 *
	 * @param name
	 *            the format name
	 * @return optional containing the format if found
	 */
	Optional<Format> findByName(String name);

	/**
	 * Creates a new format.
	 *
	 * @param format
	 *            the format to create
	 * @return the created format
	 */
	Format create(Format format);

	/**
	 * Updates an existing format.
	 *
	 * @param id
	 *            the format ID
	 * @param format
	 *            the updated format data
	 * @return the updated format
	 */
	Format update(Long id, Format format);

	/**
	 * Deletes a format by its ID.
	 *
	 * @param id
	 *            the format ID
	 * @return true if deleted, false if not found
	 */
	boolean deleteById(Long id);

	/**
	 * Retrieves all cards legal in a specific format with pagination.
	 *
	 * @param formatId
	 *            the format ID
	 * @param pageSize
	 *            the number of cards per page
	 * @param pageNumber
	 *            the page number (0-indexed)
	 * @return list of cards legal in the format
	 */
	List<Card> findCardsByFormatId(Long formatId, int pageSize, int pageNumber);

	/**
	 * Checks if a card is legal in a specific format.
	 *
	 * @param cardId
	 *            the card ID
	 * @param formatId
	 *            the format ID
	 * @return true if the card is legal, false otherwise
	 */
	boolean isCardLegal(Long cardId, Long formatId);

	/**
	 * Checks if a card is restricted in a specific format.
	 *
	 * @param cardId
	 *            the card ID
	 * @param formatId
	 *            the format ID
	 * @return true if the card is restricted, false otherwise
	 */
	boolean isCardRestricted(Long cardId, Long formatId);

	/**
	 * Retrieves the list of restricted cards for a specific format.
	 *
	 * @param formatId
	 *            the format ID
	 * @return list of restricted card names in the format
	 */
	List<String> getRestrictedCards(Long formatId);
}