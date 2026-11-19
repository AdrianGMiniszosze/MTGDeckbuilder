package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.AccountActionTokenRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DuplicateUserException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidUserDataException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UserNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.UserEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.AccountActionPurpose;
import com.deckbuilder.mtgdeckbuilder.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final AccountActionTokenRepository accountActionTokenRepository;
	private final UserEntityMapper userEntityMapper;
	private final DeckService deckService;
	private final PasswordHasher passwordHasher;

	@Override
	public List<User> findAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.userEntityMapper.toModelList(this.userRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<User> findById(Long id) {
		return this.userRepository.findById(id).map(this.userEntityMapper::toModel);
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return this.userRepository.findByUsername(username).map(this.userEntityMapper::toModel);
	}

	@Override
	public Optional<User> findByUsernameOrEmail(String identifier) {
		return this.userRepository.findByUsername(identifier).or(() -> this.userRepository.findByEmail(identifier))
				.map(this.userEntityMapper::toModel);
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return this.userRepository.findByEmail(email).map(this.userEntityMapper::toModel);
	}

	@Override
	@Transactional
	public User create(User user) {
		return createUser(user, true);
	}

	@Override
	@Transactional
	public User createUnverified(User user) {
		return createUser(user, false);
	}

	private User createUser(User user, boolean emailVerified) {
		if (user.getHashedPassword() == null || user.getHashedPassword().isBlank()) {
			throw new InvalidUserDataException("hashed_password is required to create a user");
		}
		if (this.userRepository.existsByUsername(user.getUsername())) {
			throw new DuplicateUserException("username", user.getUsername());
		}
		if (this.userRepository.existsByEmail(user.getEmail())) {
			throw new DuplicateUserException("email", user.getEmail());
		}
		UserEntity entity = this.userEntityMapper.toEntity(user);
		entity.setHashedPassword(this.passwordHasher.hash(user.getHashedPassword()));
		entity.setRole("USER");
		entity.setIsActive(true);
		entity.setEmailVerifiedAt(emailVerified ? LocalDateTime.now() : null);
		entity = this.userRepository.save(entity);
		return this.userEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public User update(Long id, User user) {
		final UserEntity existing = this.userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		if (this.userRepository.existsByUsernameAndIdNot(user.getUsername(), id)) {
			throw new DuplicateUserException("username", user.getUsername());
		}
		if (this.userRepository.existsByEmailAndIdNot(user.getEmail(), id)) {
			throw new DuplicateUserException("email", user.getEmail());
		}
		UserEntity entity = this.userEntityMapper.toEntity(user);
		entity.setId(id);
		if (entity.getHashedPassword() == null || entity.getHashedPassword().isBlank()) {
			entity.setHashedPassword(existing.getHashedPassword());
		} else {
			entity.setHashedPassword(this.passwordHasher.hash(entity.getHashedPassword()));
		}
		entity.setRole(existing.getRole());
		entity.setIsActive(existing.getIsActive());
		entity.setLastLogin(existing.getLastLogin());
		entity.setCreatedAt(existing.getCreatedAt());
		final boolean emailChanged = !Objects.equals(existing.getEmail(), entity.getEmail());
		entity.setEmailVerifiedAt(!emailChanged
				? existing.getEmailVerifiedAt()
				: null);
		if (emailChanged) {
			final LocalDateTime now = LocalDateTime.now();
			for (AccountActionPurpose purpose : AccountActionPurpose.values()) {
				this.accountActionTokenRepository.consumeActive(id, purpose, now);
			}
		}
		entity = this.userRepository.save(entity);
		return this.userEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public User recordLogin(Long id) {
		final UserEntity entity = this.userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		entity.setLastLogin(LocalDateTime.now());
		return this.userEntityMapper.toModel(this.userRepository.save(entity));
	}

	@Override
	@Transactional
	public User verifyEmail(Long id) {
		final UserEntity entity = this.userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		if (entity.getEmailVerifiedAt() == null) {
			entity.setEmailVerifiedAt(LocalDateTime.now());
			this.userRepository.save(entity);
		}
		return this.userEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public User resetPassword(Long id, String rawPassword) {
		final UserEntity entity = this.userRepository.findById(id)
				.orElseThrow(() -> new UserNotFoundException(id));
		entity.setHashedPassword(this.passwordHasher.hash(rawPassword));
		if (entity.getEmailVerifiedAt() == null) {
			entity.setEmailVerifiedAt(LocalDateTime.now());
		}
		return this.userEntityMapper.toModel(this.userRepository.save(entity));
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		this.userRepository.deleteById(id);
	}

	public boolean existsByUsername(String username) {
		return this.userRepository.existsByUsername(username);
	}

	public boolean existsByEmail(String email) {
		return this.userRepository.existsByEmail(email);
	}

	@Override
	public List<Deck> getUserDecks(Long userId, int pageSize, int pageNumber) {
		return this.deckService.findByUserId(userId, pageSize, pageNumber);
	}
}
