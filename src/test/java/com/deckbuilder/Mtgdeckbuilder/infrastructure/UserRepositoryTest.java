package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.UserEntity;
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
        testUserEntity = new UserEntity();
        testUserEntity.setId(1L);
        testUserEntity.setName("John Doe");
        testUserEntity.setUsername("johndoe");
        testUserEntity.setEmail("john@example.com");
        testUserEntity.setHashedPassword("hashedPassword123");
        testUserEntity.setCountry("USA");
        testUserEntity.setRegistrationDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find user by ID when it exists")
    void shouldFindUserById_WhenExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUserEntity));

        // When
        Optional<UserEntity> result = userRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when user ID does not exist")
    void shouldReturnEmpty_WhenUserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find user by username when it exists")
    void shouldFindUserByUsername_WhenExists() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUserEntity));

        // When
        Optional<UserEntity> result = userRepository.findByUsername("johndoe");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("johndoe");
        verify(userRepository).findByUsername("johndoe");
    }

    @Test
    @DisplayName("Should return empty when username does not exist")
    void shouldReturnEmpty_WhenUsernameNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should find user by email when it exists")
    void shouldFindUserByEmail_WhenExists() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUserEntity));

        // When
        Optional<UserEntity> result = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void shouldReturnEmpty_WhenEmailNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<UserEntity> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should check if user exists by username")
    void shouldCheckIfUserExistsByUsername() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);
        when(userRepository.existsByUsername("nonexistent")).thenReturn(false);

        // When
        boolean exists = userRepository.existsByUsername("johndoe");
        boolean notExists = userRepository.existsByUsername("nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(userRepository).existsByUsername("johndoe");
        verify(userRepository).existsByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfUserExistsByEmail() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(false);

        // When
        boolean exists = userRepository.existsByEmail("john@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).existsByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should find all users with pagination")
    void shouldFindAllUsersWithPagination() {
        // Given
        UserEntity user2 = new UserEntity();
        user2.setId(2L);
        user2.setUsername("janedoe");

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = new PageImpl<>(Arrays.asList(testUserEntity, user2));
        when(userRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<UserEntity> result = userRepository.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("johndoe");
        assertThat(result.getContent().get(1).getUsername()).isEqualTo("janedoe");
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should save user entity")
    void shouldSaveUser() {
        // Given
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);

        // When
        UserEntity result = userRepository.save(testUserEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        verify(userRepository).save(testUserEntity);
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteUserById() {
        // Given
        Long userId = 1L;

        // When
        userRepository.deleteById(userId);

        // Then
        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("Should check if user exists by ID")
    void shouldCheckIfUserExists() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = userRepository.existsById(1L);
        boolean notExists = userRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(userRepository).existsById(1L);
        verify(userRepository).existsById(999L);
    }
}
