package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.User;
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
import java.util.Collections;
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
        this.testUser = new User();
        this.testUser.setId(1L);
        this.testUser.setName("John Doe");
        this.testUser.setUsername("johndoe");
        this.testUser.setEmail("john@example.com");

        this.testUserDTO = UserDTO.builder().id(1).name("John Doe").username("johndoe").email("john@example.com").build();
	}

	@Test
	@DisplayName("Should list all users with pagination")
	void shouldListAllUsers() {
		// Given
		final User user2 = new User();
		user2.setId(2L);
		user2.setUsername("janedoe");

		final UserDTO userDTO2 = UserDTO.builder().id(2).username("janedoe").build();

		final List<User> users = Arrays.asList(this.testUser, user2);
		final List<UserDTO> userDTOs = Arrays.asList(this.testUserDTO, userDTO2);

		when(this.userService.findAll(10, 0)).thenReturn(users);
		when(this.userMapper.toUserDTOs(users)).thenReturn(userDTOs);

		// When
		final ResponseEntity<List<UserDTO>> response = this.userController.listUsers(10, 0);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getUsername()).isEqualTo("johndoe");
		verify(this.userService).findAll(10, 0);
	}

	@Test
	@DisplayName("Should get user by ID when it exists")
	void shouldGetUserById_WhenExists() {
		// Given
		when(this.userService.findById(1L)).thenReturn(Optional.of(this.testUser));
		when(this.userMapper.toUserDTO(this.testUser)).thenReturn(this.testUserDTO);

		// When
		final ResponseEntity<UserDTO> response = this.userController.getUserById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
		verify(this.userService).findById(1L);
	}

	@Test
	@DisplayName("Should return 404 when user not found")
	void shouldReturn404_WhenUserNotFound() {
		// Given
		when(this.userService.findById(999L)).thenReturn(Optional.empty());

		// When
		final ResponseEntity<UserDTO> response = this.userController.getUserById(999);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
		verify(this.userService).findById(999L);
	}

	@Test
	@DisplayName("Should create new user")
	void shouldCreateUser() {
		// Given
		when(this.userMapper.toUser(this.testUserDTO)).thenReturn(this.testUser);
		when(this.userService.create(this.testUser)).thenReturn(this.testUser);
		when(this.userMapper.toUserDTO(this.testUser)).thenReturn(this.testUserDTO);

		// When
		final ResponseEntity<UserDTO> response = this.userController.createUser(this.testUserDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
		verify(this.userService).create(this.testUser);
	}

	@Test
	@DisplayName("Should update existing user")
	void shouldUpdateUser() {
		// Given
		when(this.userMapper.toUser(this.testUserDTO)).thenReturn(this.testUser);
		when(this.userService.update(1L, this.testUser)).thenReturn(this.testUser);
		when(this.userMapper.toUserDTO(this.testUser)).thenReturn(this.testUserDTO);

		// When
		final ResponseEntity<UserDTO> response = this.userController.updateUser(1, this.testUserDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
		verify(this.userService).update(1L, this.testUser);
	}

	@Test
	@DisplayName("Should delete user by ID")
	void shouldDeleteUser() {
		// When
		final ResponseEntity<Void> response = this.userController.deleteUser(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.userService).deleteById(1L);
	}

	@Test
	@DisplayName("Should get user decks")
	void shouldGetUserDecks() {
		// Given
		final Deck deck1 = new Deck();
		deck1.setId(1L);
		deck1.setName("Blue Control");

		final Deck deck2 = new Deck();
		deck2.setId(2L);
		deck2.setName("Red Aggro");

		final DeckDTO deckDTO1 = DeckDTO.builder().id(1).deck_name("Blue Control").build();

		final DeckDTO deckDTO2 = DeckDTO.builder().id(2).deck_name("Red Aggro").build();

		when(this.userService.getUserDecks(1L, 10, 0)).thenReturn(Arrays.asList(deck1, deck2));
		when(this.deckMapper.toDecksDTO(anyList())).thenReturn(Arrays.asList(deckDTO1, deckDTO2));

		// When
		final ResponseEntity<List<DeckDTO>> response = this.userController.getUserDecks(1, 10, 0);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getDeck_name()).isEqualTo("Blue Control");
		assertThat(response.getBody().get(1).getDeck_name()).isEqualTo("Red Aggro");
		verify(this.userService).getUserDecks(1L, 10, 0);
	}

	@Test
	@DisplayName("Should use default pagination when parameters are null")
	void shouldUseDefaultPagination_WhenParametersAreNull() {
		// Given
		when(this.userService.findAll(10, 0)).thenReturn(Collections.singletonList(this.testUser));
		when(this.userMapper.toUserDTOs(anyList())).thenReturn(Collections.singletonList(this.testUserDTO));

		// When
		final ResponseEntity<List<UserDTO>> response = this.userController.listUsers(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.userService).findAll(10, 0);
	}

	@Test
	@DisplayName("Should use default pagination for getUserDecks when parameters are null")
	void shouldUseDefaultPaginationForDecks_WhenParametersAreNull() {
		// Given
		final Deck deck = new Deck();
		deck.setId(1L);

		final DeckDTO deckDTO = DeckDTO.builder().id(1).build();

		when(this.userService.getUserDecks(1L, 10, 0)).thenReturn(List.of(deck));
		when(this.deckMapper.toDecksDTO(anyList())).thenReturn(Collections.singletonList(deckDTO));

		// When
		final ResponseEntity<List<DeckDTO>> response = this.userController.getUserDecks(1, null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.userService).getUserDecks(1L, 10, 0);
	}
}
