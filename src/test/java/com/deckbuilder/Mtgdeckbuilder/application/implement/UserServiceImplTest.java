package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.UserEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserEntityMapper userEntityMapper;

	@Mock
	private DeckService deckService;

	@InjectMocks
	private UserServiceImpl userService;

	private UserEntity testUserEntity;
	private User testUser;

	@BeforeEach
	void setUp() {
        this.testUserEntity = new UserEntity();
        this.testUserEntity.setId(1L);
        this.testUserEntity.setName("John Doe");
        this.testUserEntity.setUsername("johndoe");
        this.testUserEntity.setEmail("john@example.com");
        this.testUserEntity.setHashedPassword("hashedPassword123");
        this.testUserEntity.setCountry("USA");
        this.testUserEntity.setRegistrationDate(LocalDateTime.now());

        this.testUser = new User();
        this.testUser.setId(1L);
        this.testUser.setName("John Doe");
        this.testUser.setUsername("johndoe");
        this.testUser.setEmail("john@example.com");
        this.testUser.setHashedPassword("hashedPassword123");
        this.testUser.setCountry("USA");
	}

	@Test
	@DisplayName("Should find all users with pagination")
	void shouldFindAllUsers() {
		// Given
		final UserEntity user2 = new UserEntity();
		user2.setId(2L);
		user2.setUsername("janedoe");

		final User userModel2 = new User();
		userModel2.setId(2L);
		userModel2.setUsername("janedoe");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<UserEntity> entityPage = new PageImpl<>(Arrays.asList(this.testUserEntity, user2));
		when(this.userRepository.findAll(pageable)).thenReturn(entityPage);
		when(this.userEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(this.testUser, userModel2));

		// When
		final List<User> result = this.userService.findAll(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getUsername()).isEqualTo("johndoe");
		assertThat(result.get(1).getUsername()).isEqualTo("janedoe");
		verify(this.userRepository).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should find user by ID when it exists")
	void shouldFindUserById_WhenExists() {
		// Given
		when(this.userRepository.findById(1L)).thenReturn(Optional.of(this.testUserEntity));
		when(this.userEntityMapper.toModel(this.testUserEntity)).thenReturn(this.testUser);

		// When
		final Optional<User> result = this.userService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getUsername()).isEqualTo("johndoe");
		assertThat(result.get().getEmail()).isEqualTo("john@example.com");
		verify(this.userRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when user ID does not exist")
	void shouldReturnEmpty_WhenUserNotFound() {
		// Given
		when(this.userRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<User> result = this.userService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findById(999L);
		verify(this.userEntityMapper, never()).toModel(any(UserEntity.class));
	}

	@Test
	@DisplayName("Should find user by username when it exists")
	void shouldFindUserByUsername_WhenExists() {
		// Given
		when(this.userRepository.findByUsername("johndoe")).thenReturn(Optional.of(this.testUserEntity));
		when(this.userEntityMapper.toModel(this.testUserEntity)).thenReturn(this.testUser);

		// When
		final Optional<User> result = this.userService.findByUsername("johndoe");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getUsername()).isEqualTo("johndoe");
		verify(this.userRepository).findByUsername("johndoe");
	}

	@Test
	@DisplayName("Should return empty when username does not exist")
	void shouldReturnEmpty_WhenUsernameNotFound() {
		// Given
		when(this.userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

		// When
		final Optional<User> result = this.userService.findByUsername("nonexistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findByUsername("nonexistent");
		verify(this.userEntityMapper, never()).toModel(any(UserEntity.class));
	}

	@Test
	@DisplayName("Should find user by email when it exists")
	void shouldFindUserByEmail_WhenExists() {
		// Given
		when(this.userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(this.testUserEntity));
		when(this.userEntityMapper.toModel(this.testUserEntity)).thenReturn(this.testUser);

		// When
		final Optional<User> result = this.userService.findByEmail("john@example.com");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getEmail()).isEqualTo("john@example.com");
		verify(this.userRepository).findByEmail("john@example.com");
	}

	@Test
	@DisplayName("Should return empty when email does not exist")
	void shouldReturnEmpty_WhenEmailNotFound() {
		// Given
		when(this.userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

		// When
		final Optional<User> result = this.userService.findByEmail("nonexistent@example.com");

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findByEmail("nonexistent@example.com");
		verify(this.userEntityMapper, never()).toModel(any(UserEntity.class));
	}

	@Test
	@DisplayName("Should check if user exists by username")
	void shouldCheckIfUserExistsByUsername() {
		// Given
		when(this.userRepository.existsByUsername("johndoe")).thenReturn(true);

		// When
		final boolean result = this.userService.existsByUsername("johndoe");

		// Then
		assertThat(result).isTrue();
		verify(this.userRepository).existsByUsername("johndoe");
	}

	@Test
	@DisplayName("Should check if user exists by email")
	void shouldCheckIfUserExistsByEmail() {
		// Given
		when(this.userRepository.existsByEmail("john@example.com")).thenReturn(true);

		// When
		final boolean result = this.userService.existsByEmail("john@example.com");

		// Then
		assertThat(result).isTrue();
		verify(this.userRepository).existsByEmail("john@example.com");
	}

	@Test
	@DisplayName("Should create new user")
	void shouldCreateUser() {
		// Given
		when(this.userEntityMapper.toEntity(this.testUser)).thenReturn(this.testUserEntity);
		when(this.userRepository.save(this.testUserEntity)).thenReturn(this.testUserEntity);
		when(this.userEntityMapper.toModel(this.testUserEntity)).thenReturn(this.testUser);

		// When
		final User result = this.userService.create(this.testUser);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo("johndoe");
		verify(this.userRepository).save(this.testUserEntity);
	}

	@Test
	@DisplayName("Should update existing user")
	void shouldUpdateUser_WhenExists() {
		// Given
		when(this.userRepository.existsById(1L)).thenReturn(true);
		when(this.userEntityMapper.toEntity(this.testUser)).thenReturn(this.testUserEntity);
		when(this.userRepository.save(any(UserEntity.class))).thenReturn(this.testUserEntity);
		when(this.userEntityMapper.toModel(this.testUserEntity)).thenReturn(this.testUser);

		// When
		final User result = this.userService.update(1L, this.testUser);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo("johndoe");
		verify(this.userRepository).existsById(1L);
		verify(this.userRepository).save(any(UserEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent user")
	void shouldThrowException_WhenUpdatingNonExistentUser() {
		// Given
		when(this.userRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.userService.update(999L, this.testUser)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("User not found with id: 999");

		verify(this.userRepository).existsById(999L);
		verify(this.userRepository, never()).save(any(UserEntity.class));
	}

	@Test
	@DisplayName("Should delete user by ID")
	void shouldDeleteUserById() {
		// Given
		final Long userId = 1L;

		// When
        this.userService.deleteById(userId);

		// Then
		verify(this.userRepository).deleteById(userId);
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

		when(this.deckService.findByUserId(1L, 10, 0)).thenReturn(Arrays.asList(deck1, deck2));

		// When
		final List<Deck> result = this.userService.getUserDecks(1L, 10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Blue Control");
		assertThat(result.get(1).getName()).isEqualTo("Red Aggro");
		verify(this.deckService).findByUserId(1L, 10, 0);
	}
}
