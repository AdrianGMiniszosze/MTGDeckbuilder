package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.UsersApi;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.mtgdeckbuilder.application.UserService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {
	private final UserService userService;
	private final UserMapper userMapper;
	private final DeckMapper deckMapper;

	@Override
	public ResponseEntity<List<UserDTO>> listUsers(Integer pagesize, Integer pagenumber) {
		final var users = this.userService.findAll(pagesize != null ? pagesize : 10,
				pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.userMapper.toUserDTOs(users));
	}

	@Override
	public ResponseEntity<UserDTO> getUserById(Integer id) {
		final var user = this.userService.findById(id.longValue())
				.orElseThrow(() -> new UserNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.userMapper.toUserDTO(user));
	}

	@Override
	public ResponseEntity<UserDTO> createUser(@Valid UserDTO userDTO) {
		final var user = this.userMapper.toUser(userDTO);
		final var created = this.userService.create(user);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.userMapper.toUserDTO(created));
	}

	@Override
	public ResponseEntity<UserDTO> updateUser(Integer id, @Valid UserDTO userDTO) {
		final var user = this.userMapper.toUser(userDTO);
		final var updated = this.userService.update(id.longValue(), user);
		return ResponseEntity.ok(this.userMapper.toUserDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteUser(Integer id) {
		this.userService.deleteById(id.longValue());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<List<DeckDTO>> getUserDecks(Integer id, Integer pagesize, Integer pagenumber) {
		final var decks = this.userService.getUserDecks(id.longValue(), pagesize != null ? pagesize : 10,
				pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.deckMapper.toDecksDTO(decks));
	}
}
