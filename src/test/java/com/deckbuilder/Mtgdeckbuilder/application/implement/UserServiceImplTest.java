package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.application.DeckService;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.UserEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
import com.deckbuilder.Mtgdeckbuilder.model.User;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.UserEntity;
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
        testUserEntity = new UserEntity();
        testUserEntity.setId(1L);
        testUserEntity.setName("John Doe");
        testUserEntity.setUsername("johndoe");
        testUserEntity.setEmail("john@example.com");
        testUserEntity.setHashedPassword("hashedPassword123");
        testUserEntity.setCountry("USA");
        testUserEntity.setRegistrationDate(LocalDateTime.now());

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@example.com");
        testUser.setHashedPassword("hashedPassword123");
        testUser.setCountry("USA");
    }

    @Test
    @DisplayName("Should find all users with pagination")
    void shouldFindAllUsers() {
        // Given
        UserEntity user2 = new UserEntity();
        user2.setId(2L);
        user2.setUsername("janedoe");

        User userModel2 = new User();
        userModel2.setId(2L);
        userModel2.setUsername("janedoe");

        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> entityPage = new PageImpl<>(Arrays.asList(testUserEntity, user2));
        when(userRepository.findAll(pageable)).thenReturn(entityPage);
        when(userEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testUser, userModel2));

        // When
        List<User> result = userService.findAll(10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("johndoe");
        assertThat(result.get(1).getUsername()).isEqualTo("janedoe");
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should find user by ID when it exists")
    void shouldFindUserById_WhenExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUserEntity));
        when(userEntityMapper.toModel(testUserEntity)).thenReturn(testUser);

        // When
        Optional<User> result = userService.findById(1L);

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
        Optional<User> result = userService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(999L);
        verify(userEntityMapper, never()).toModel(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should find user by username when it exists")
    void shouldFindUserByUsername_WhenExists() {
        // Given
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUserEntity));
        when(userEntityMapper.toModel(testUserEntity)).thenReturn(testUser);

        // When
        Optional<User> result = userService.findByUsername("johndoe");

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
        Optional<User> result = userService.findByUsername("nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("nonexistent");
        verify(userEntityMapper, never()).toModel(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should find user by email when it exists")
    void shouldFindUserByEmail_WhenExists() {
        // Given
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUserEntity));
        when(userEntityMapper.toModel(testUserEntity)).thenReturn(testUser);

        // When
        Optional<User> result = userService.findByEmail("john@example.com");

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
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userEntityMapper, never()).toModel(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should check if user exists by username")
    void shouldCheckIfUserExistsByUsername() {
        // Given
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // When
        boolean result = userService.existsByUsername("johndoe");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByUsername("johndoe");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckIfUserExistsByEmail() {
        // Given
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // When
        boolean result = userService.existsByEmail("john@example.com");

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmail("john@example.com");
    }

    @Test
    @DisplayName("Should create new user")
    void shouldCreateUser() {
        // Given
        when(userEntityMapper.toEntity(testUser)).thenReturn(testUserEntity);
        when(userRepository.save(testUserEntity)).thenReturn(testUserEntity);
        when(userEntityMapper.toModel(testUserEntity)).thenReturn(testUser);

        // When
        User result = userService.create(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        verify(userRepository).save(testUserEntity);
    }

    @Test
    @DisplayName("Should update existing user")
    void shouldUpdateUser_WhenExists() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userEntityMapper.toEntity(testUser)).thenReturn(testUserEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUserEntity);
        when(userEntityMapper.toModel(testUserEntity)).thenReturn(testUser);

        // When
        User result = userService.update(1L, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        verify(userRepository).existsById(1L);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void shouldThrowException_WhenUpdatingNonExistentUser() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> userService.update(999L, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found with id: 999");
        
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteUserById() {
        // Given
        Long userId = 1L;

        // When
        userService.deleteById(userId);

        // Then
        verify(userRepository).deleteById(userId);
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

        when(deckService.findByUserId(1L, 10, 0)).thenReturn(Arrays.asList(deck1, deck2));

        // When
        List<Deck> result = userService.getUserDecks(1L, 10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Blue Control");
        assertThat(result.get(1).getName()).isEqualTo("Red Aggro");
        verify(deckService).findByUserId(1L, 10, 0);
    }
}
