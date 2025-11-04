package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.Mtgdeckbuilder.application.UserService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
import com.deckbuilder.Mtgdeckbuilder.model.User;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Controller Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DeckMapper deckMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");

        testUserDTO = UserDTO.builder()
                .id(1)
                .name("John Doe")
                .username("johndoe")
                .email("john@example.com")
                .build();
    }

    @Test
    @DisplayName("Should list all users with pagination")
    void shouldListAllUsers() {
        // Given
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("janedoe");

        UserDTO userDTO2 = UserDTO.builder()
                .id(2)
                .username("janedoe")
                .build();

        List<User> users = Arrays.asList(testUser, user2);
        List<UserDTO> userDTOs = Arrays.asList(testUserDTO, userDTO2);

        when(userService.findAll(10, 0)).thenReturn(users);
        when(userMapper.toUserDTOs(users)).thenReturn(userDTOs);

        // When
        ResponseEntity<List<UserDTO>> response = userController.listUsers(10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getUsername()).isEqualTo("johndoe");
        verify(userService).findAll(10, 0);
    }

    @Test
    @DisplayName("Should get user by ID when it exists")
    void shouldGetUserById_WhenExists() {
        // Given
        when(userService.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

        // When
        ResponseEntity<UserDTO> response = userController.getUserById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
        verify(userService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when user not found")
    void shouldReturn404_WhenUserNotFound() {
        // Given
        when(userService.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<UserDTO> response = userController.getUserById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(userService).findById(999L);
    }

    @Test
    @DisplayName("Should create new user")
    void shouldCreateUser() {
        // Given
        when(userMapper.toUser(testUserDTO)).thenReturn(testUser);
        when(userService.create(testUser)).thenReturn(testUser);
        when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

        // When
        ResponseEntity<UserDTO> response = userController.createUser(testUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
        verify(userService).create(testUser);
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateUser() {
        // Given
        when(userMapper.toUser(testUserDTO)).thenReturn(testUser);
        when(userService.update(1L, testUser)).thenReturn(testUser);
        when(userMapper.toUserDTO(testUser)).thenReturn(testUserDTO);

        // When
        ResponseEntity<UserDTO> response = userController.updateUser(1, testUserDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
        verify(userService).update(1L, testUser);
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteUser() {
        // When
        ResponseEntity<Void> response = userController.deleteUser(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(userService).deleteById(1L);
    }

    @Test
    @DisplayName("Should get user decks")
    void shouldGetUserDecks() {
        // Given
        Deck deck1 = new Deck();
        deck1.setId(1L);
        deck1.setName("Blue Control");

        Deck deck2 = new Deck();
        deck2.setId(2L);
        deck2.setName("Red Aggro");

        DeckDTO deckDTO1 = DeckDTO.builder()
                .id(1)
                .deck_name("Blue Control")
                .build();

        DeckDTO deckDTO2 = DeckDTO.builder()
                .id(2)
                .deck_name("Red Aggro")
                .build();

        when(userService.getUserDecks(1L, 10, 0)).thenReturn(Arrays.asList(deck1, deck2));
        when(deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(deckDTO1, deckDTO2));

        // When
        ResponseEntity<List<DeckDTO>> response = userController.getUserDecks(1, 10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getDeck_name()).isEqualTo("Blue Control");
        assertThat(response.getBody().get(1).getDeck_name()).isEqualTo("Red Aggro");
        verify(userService).getUserDecks(1L, 10, 0);
    }

    @Test
    @DisplayName("Should use default pagination when parameters are null")
    void shouldUseDefaultPagination_WhenParametersAreNull() {
        // Given
        when(userService.findAll(10, 0)).thenReturn(Arrays.asList(testUser));
        when(userMapper.toUserDTOs(anyList())).thenReturn(Arrays.asList(testUserDTO));

        // When
        ResponseEntity<List<UserDTO>> response = userController.listUsers(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).findAll(10, 0);
    }

    @Test
    @DisplayName("Should use default pagination for getUserDecks when parameters are null")
    void shouldUseDefaultPaginationForDecks_WhenParametersAreNull() {
        // Given
        Deck deck = new Deck();
        deck.setId(1L);
        
        DeckDTO deckDTO = DeckDTO.builder().id(1).build();
        
        when(userService.getUserDecks(1L, 10, 0)).thenReturn(Arrays.asList(deck));
        when(deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(deckDTO));

        // When
        ResponseEntity<List<DeckDTO>> response = userController.getUserDecks(1, null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userService).getUserDecks(1L, 10, 0);
    }
}
