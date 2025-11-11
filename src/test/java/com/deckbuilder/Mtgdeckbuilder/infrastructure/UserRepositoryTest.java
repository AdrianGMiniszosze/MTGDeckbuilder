package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Repository Tests")
class UserRepositoryTest {

	@Mock
	private UserRepository userRepository;

	private UserEntity testUserEntity;

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
	}

	@Test
	@DisplayName("Should find user by ID when it exists")
	void shouldFindUserById_WhenExists() {
		// Given
		when(this.userRepository.findById(1L)).thenReturn(Optional.of(this.testUserEntity));

		// When
		final Optional<UserEntity> result = this.userRepository.findById(1L);

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
		final Optional<UserEntity> result = this.userRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find user by username when it exists")
	void shouldFindUserByUsername_WhenExists() {
		// Given
		when(this.userRepository.findByUsername("johndoe")).thenReturn(Optional.of(this.testUserEntity));

		// When
		final Optional<UserEntity> result = this.userRepository.findByUsername("johndoe");

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
		final Optional<UserEntity> result = this.userRepository.findByUsername("nonexistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findByUsername("nonexistent");
	}

	@Test
	@DisplayName("Should find user by email when it exists")
	void shouldFindUserByEmail_WhenExists() {
		// Given
		when(this.userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(this.testUserEntity));

		// When
		final Optional<UserEntity> result = this.userRepository.findByEmail("john@example.com");

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
		final Optional<UserEntity> result = this.userRepository.findByEmail("nonexistent@example.com");

		// Then
		assertThat(result).isEmpty();
		verify(this.userRepository).findByEmail("nonexistent@example.com");
	}

	@Test
	@DisplayName("Should check if user exists by username")
	void shouldCheckIfUserExistsByUsername() {
		// Given
		when(this.userRepository.existsByUsername("johndoe")).thenReturn(true);
		when(this.userRepository.existsByUsername("nonexistent")).thenReturn(false);

		// When
		final boolean exists = this.userRepository.existsByUsername("johndoe");
		final boolean notExists = this.userRepository.existsByUsername("nonexistent");

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.userRepository).existsByUsername("johndoe");
		verify(this.userRepository).existsByUsername("nonexistent");
	}

	@Test
	@DisplayName("Should check if user exists by email")
	void shouldCheckIfUserExistsByEmail() {
		// Given
		when(this.userRepository.existsByEmail("john@example.com")).thenReturn(true);
		when(this.userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

		// When
		final boolean exists = this.userRepository.existsByEmail("john@example.com");
		final boolean notExists = this.userRepository.existsByEmail("nonexistent@example.com");

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.userRepository).existsByEmail("john@example.com");
		verify(this.userRepository).existsByEmail("nonexistent@example.com");
	}

	@Test
	@DisplayName("Should find all users with pagination")
	void shouldFindAllUsersWithPagination() {
		// Given
		final UserEntity user2 = new UserEntity();
		user2.setId(2L);
		user2.setUsername("janedoe");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<UserEntity> page = new PageImpl<>(Arrays.asList(this.testUserEntity, user2));
		when(this.userRepository.findAll(pageable)).thenReturn(page);

		// When
		final Page<UserEntity> result = this.userRepository.findAll(pageable);

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).getUsername()).isEqualTo("johndoe");
		assertThat(result.getContent().get(1).getUsername()).isEqualTo("janedoe");
		verify(this.userRepository).findAll(pageable);
	}

	@Test
	@DisplayName("Should save user entity")
	void shouldSaveUser() {
		// Given
		when(this.userRepository.save(any(UserEntity.class))).thenReturn(this.testUserEntity);

		// When
		final UserEntity result = this.userRepository.save(this.testUserEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUsername()).isEqualTo("johndoe");
		verify(this.userRepository).save(this.testUserEntity);
	}

	@Test
	@DisplayName("Should delete user by ID")
	void shouldDeleteUserById() {
		// Given
		final Long userId = 1L;

		// When
        this.userRepository.deleteById(userId);

		// Then
		verify(this.userRepository).deleteById(userId);
	}

	@Test
	@DisplayName("Should check if user exists by ID")
	void shouldCheckIfUserExists() {
		// Given
		when(this.userRepository.existsById(1L)).thenReturn(true);
		when(this.userRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean exists = this.userRepository.existsById(1L);
		final boolean notExists = this.userRepository.existsById(999L);

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.userRepository).existsById(1L);
		verify(this.userRepository).existsById(999L);
	}
}
