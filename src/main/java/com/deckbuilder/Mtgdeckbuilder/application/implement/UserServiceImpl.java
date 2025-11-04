package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.model.User;
import com.deckbuilder.Mtgdeckbuilder.model.Deck;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.UserEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.UserRepository;
import com.deckbuilder.Mtgdeckbuilder.application.UserService;
import com.deckbuilder.Mtgdeckbuilder.application.DeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return userEntityMapper.toModelList(userRepository.findAll(pageable).getContent());
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(userEntityMapper::toModel);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(userEntityMapper::toModel);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(userEntityMapper::toModel);
    }

    @Override
    public User create(User user) {
        UserEntity entity = userEntityMapper.toEntity(user);
        entity = userRepository.save(entity);
        return userEntityMapper.toModel(entity);
    }

    @Override
    public User update(Long id, User user) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        UserEntity entity = userEntityMapper.toEntity(user);
        entity.setId(id);
        entity = userRepository.save(entity);
        return userEntityMapper.toModel(entity);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<Deck> getUserDecks(Long userId, int pageSize, int pageNumber) {
        return deckService.findByUserId(userId, pageSize, pageNumber);
    }
}