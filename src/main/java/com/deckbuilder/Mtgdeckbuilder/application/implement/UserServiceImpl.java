package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UserNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.UserEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	private final UserRepository userRepository;
	private final UserEntityMapper userEntityMapper;
	private final DeckService deckService;

	@Override
	public List<User> findAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.userEntityMapper.toModelList(this.userRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<User> findById(Long id) {
		return this.userRepository.findById(id).map(this.userEntityMapper::toModel);
	}

	public Optional<User> findByUsername(String username) {
		return this.userRepository.findByUsername(username).map(this.userEntityMapper::toModel);
	}

	public Optional<User> findByEmail(String email) {
		return this.userRepository.findByEmail(email).map(this.userEntityMapper::toModel);
	}

	@Override
	@Transactional
	public User create(User user) {
		UserEntity entity = this.userEntityMapper.toEntity(user);
		entity = this.userRepository.save(entity);
		return this.userEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public User update(Long id, User user) {
		if (!this.userRepository.existsById(id)) {
			throw new UserNotFoundException(id);
		}
		UserEntity entity = this.userEntityMapper.toEntity(user);
		entity.setId(id);
		entity = this.userRepository.save(entity);
		return this.userEntityMapper.toModel(entity);
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