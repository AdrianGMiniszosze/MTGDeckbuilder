package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.Mtgdeckbuilder.application.CardService;
import com.deckbuilder.Mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import com.deckbuilder.Mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Controller Tests")
class CardControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private CardTagService cardTagService;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardTagMapper cardTagMapper;

    @InjectMocks
    private CardController cardController;

    private Card testCard;
    private CardDTO testCardDTO;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .id(1L)
                .name("Lightning Bolt")
                .manaCost("{R}")
                .typeLine("Instant")
                .oracleText("Lightning Bolt deals 3 damage to any target.")
                .rarity("Common")
                .build();

        testCardDTO = CardDTO.builder()
                .id(1)
                .card_name("Lightning Bolt")
                .mana_cost("{R}")
                .card_type("Instant")
                .card_text("Lightning Bolt deals 3 damage to any target.")
                .rarity("Common")
                .build();
    }

    @Test
    @DisplayName("Should list all cards")
    void shouldListAllCards() {
        // Given
        Card card2 = Card.builder().id(2L).name("Counterspell").build();
        CardDTO cardDTO2 = CardDTO.builder()
                .id(2)
                .card_name("Counterspell")
                .build();

        when(cardService.getAllCards(10, 0)).thenReturn(Arrays.asList(testCard, card2));
        when(cardMapper.toDto(testCard)).thenReturn(testCardDTO);
        when(cardMapper.toDto(card2)).thenReturn(cardDTO2);

        // When
        ResponseEntity<List<CardDTO>> response = cardController.listCards(10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getCard_name()).isEqualTo("Lightning Bolt");
        assertThat(response.getBody().get(1).getCard_name()).isEqualTo("Counterspell");
        verify(cardService, times(1)).getAllCards(10, 0);
        verify(cardMapper, times(2)).toDto(any(Card.class));
    }

    @Test
    @DisplayName("Should list cards with default pagination")
    void shouldListCardsWithDefaultPagination() {
        // Given
        when(cardService.getAllCards(10, 0)).thenReturn(List.of(testCard));
        when(cardMapper.toDto(testCard)).thenReturn(testCardDTO);

        // When
        ResponseEntity<List<CardDTO>> response = cardController.listCards(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cardService, times(1)).getAllCards(10, 0);
    }

    @Test
    @DisplayName("Should get card by ID when exists")
    void shouldGetCardByIdWhenExists() {
        // Given
        when(cardService.getCardById(1L)).thenReturn(Optional.of(testCard));
        when(cardMapper.toDto(testCard)).thenReturn(testCardDTO);

        // When
        ResponseEntity<CardDTO> response = cardController.getCardById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCard_name()).isEqualTo("Lightning Bolt");
        verify(cardService, times(1)).getCardById(1L);
        verify(cardMapper, times(1)).toDto(testCard);
    }

    @Test
    @DisplayName("Should return 404 when card not found")
    void shouldReturn404WhenCardNotFound() {
        // Given
        when(cardService.getCardById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<CardDTO> response = cardController.getCardById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(cardService, times(1)).getCardById(999L);
        verify(cardMapper, never()).toDto(any(Card.class));
    }

    @Test
    @DisplayName("Should create card")
    void shouldCreateCard() {
        // Given
        Card newCard = Card.builder().name("New Card").build();
        Card createdCard = Card.builder().id(3L).name("New Card").build();
        CardDTO newCardDTO = CardDTO.builder().card_name("New Card").build();
        CardDTO createdCardDTO = CardDTO.builder()
                .id(3)
                .card_name("New Card")
                .build();

        when(cardMapper.toEntity(newCardDTO)).thenReturn(newCard);
        when(cardService.createCard(newCard)).thenReturn(createdCard);
        when(cardMapper.toDto(createdCard)).thenReturn(createdCardDTO);

        // When
        ResponseEntity<CardDTO> response = cardController.createCard(newCardDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3);
        assertThat(response.getBody().getCard_name()).isEqualTo("New Card");
        verify(cardMapper, times(1)).toEntity(newCardDTO);
        verify(cardService, times(1)).createCard(newCard);
        verify(cardMapper, times(1)).toDto(createdCard);
    }

    @Test
    @DisplayName("Should update card when exists")
    void shouldUpdateCardWhenExists() {
        // Given
        CardDTO updatedDTO = CardDTO.builder()
                .id(1)
                .card_name("Lightning Bolt Updated")
                .build();
        
        Card updatedCard = Card.builder().name("Lightning Bolt Updated").build();
        Card savedCard = Card.builder().id(1L).name("Lightning Bolt Updated").build();
        CardDTO savedDTO = CardDTO.builder()
                .id(1)
                .card_name("Lightning Bolt Updated")
                .build();

        when(cardMapper.toEntity(updatedDTO)).thenReturn(updatedCard);
        when(cardService.updateCard(1L, updatedCard)).thenReturn(Optional.of(savedCard));
        when(cardMapper.toDto(savedCard)).thenReturn(savedDTO);

        // When
        ResponseEntity<CardDTO> response = cardController.updateCard(1, updatedDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCard_name()).isEqualTo("Lightning Bolt Updated");
        verify(cardMapper, times(1)).toEntity(updatedDTO);
        verify(cardService, times(1)).updateCard(1L, updatedCard);
        verify(cardMapper, times(1)).toDto(savedCard);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent card")
    void shouldReturn404WhenUpdatingNonExistentCard() {
        // Given
        CardDTO updatedDTO = CardDTO.builder().card_name("Non-existent").build();
        Card card = Card.builder().name("Non-existent").build();

        when(cardMapper.toEntity(updatedDTO)).thenReturn(card);
        when(cardService.updateCard(999L, card)).thenReturn(Optional.empty());

        // When
        ResponseEntity<CardDTO> response = cardController.updateCard(999, updatedDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(cardService, times(1)).updateCard(999L, card);
        verify(cardMapper, never()).toDto(any(Card.class));
    }

    @Test
    @DisplayName("Should delete card")
    void shouldDeleteCard() {
        // Given
        doNothing().when(cardService).deleteCard(1L);

        // When
        ResponseEntity<Void> response = cardController.deleteCard(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(cardService, times(1)).deleteCard(1L);
    }

    @Test
    @DisplayName("Should list tags for card")
    void shouldListTagsForCard() {
        // Given
        CardTag tag1 = CardTag.builder()
                .cardId(1L)
                .tagId(1L)
                .weight(0.9)
                .build();
        CardTag tag2 = CardTag.builder()
                .cardId(1L)
                .tagId(2L)
                .weight(0.8)
                .build();
        List<CardTag> tags = Arrays.asList(tag1, tag2);

        CardTagDTO tagDTO1 = CardTagDTO.builder()
                .card_id(1)
                .tag_id(1)
                .build();
        CardTagDTO tagDTO2 = CardTagDTO.builder()
                .card_id(1)
                .tag_id(2)
                .build();
        List<CardTagDTO> tagDTOs = Arrays.asList(tagDTO1, tagDTO2);

        when(cardTagService.findByCardId(1L, 10, 0)).thenReturn(tags);
        when(cardTagMapper.toCardTagDTOs(tags)).thenReturn(tagDTOs);

        // When
        ResponseEntity<List<CardTagDTO>> response = cardController.listTagsForCard(1, 10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(cardTagService, times(1)).findByCardId(1L, 10, 0);
        verify(cardTagMapper, times(1)).toCardTagDTOs(tags);
    }

    @Test
    @DisplayName("Should update card tag when exists")
    void shouldUpdateCardTagWhenExists() {
        // Given
        CardTagDTO tagDTO = CardTagDTO.builder()
                .card_id(1)
                .tag_id(1)
                .build();
        
        CardTag tag = CardTag.builder().cardId(1L).tagId(1L).build();
        CardTag updatedTag = CardTag.builder().cardId(1L).tagId(1L).weight(0.95).build();
        CardTagDTO updatedDTO = CardTagDTO.builder()
                .card_id(1)
                .tag_id(1)
                .build();

        when(cardTagMapper.toCardTag(tagDTO)).thenReturn(tag);
        when(cardTagService.updateCardTag(1L, 1L, tag)).thenReturn(Optional.of(updatedTag));
        when(cardTagMapper.toCardTagDTO(updatedTag)).thenReturn(updatedDTO);

        // When
        ResponseEntity<CardTagDTO> response = cardController.updateCardTag(1, 1, tagDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cardTagService, times(1)).updateCardTag(1L, 1L, tag);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent card tag")
    void shouldReturn404WhenUpdatingNonExistentCardTag() {
        // Given
        CardTagDTO tagDTO = CardTagDTO.builder().build();
        CardTag tag = CardTag.builder().build();

        when(cardTagMapper.toCardTag(tagDTO)).thenReturn(tag);
        when(cardTagService.updateCardTag(999L, 999L, tag)).thenReturn(Optional.empty());

        // When
        ResponseEntity<CardTagDTO> response = cardController.updateCardTag(999, 999, tagDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(cardTagService, times(1)).updateCardTag(999L, 999L, tag);
    }

    @Test
    @DisplayName("Should delete card tag")
    void shouldDeleteCardTag() {
        // Given
        doNothing().when(cardTagService).deleteCardTag(1L, 1L);

        // When
        ResponseEntity<Void> response = cardController.deleteCardTag(1, 1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cardTagService, times(1)).deleteCardTag(1L, 1L);
    }
}
