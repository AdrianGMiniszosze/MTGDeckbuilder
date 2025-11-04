package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.UsersApi;
import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.Mtgdeckbuilder.application.UserService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.UserMapper;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.DeckMapper;
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
        var users = userService.findAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        return ResponseEntity.ok(userMapper.toUserDTOs(users));
    }

    @Override
    public ResponseEntity<UserDTO> getUserById(Integer id) {
        return userService.findById(id.longValue())
                         .map(user -> ResponseEntity.ok(userMapper.toUserDTO(user)))
                         .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<UserDTO> createUser(@Valid UserDTO userDTO) {
        var user = userMapper.toUser(userDTO);
        var created = userService.create(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                           .body(userMapper.toUserDTO(created));
    }

    @Override
    public ResponseEntity<UserDTO> updateUser(Integer id, @Valid UserDTO userDTO) {
        var user = userMapper.toUser(userDTO);
        var updated = userService.update(id.longValue(), user);
        return ResponseEntity.ok(userMapper.toUserDTO(updated));
    }

    @Override
    public ResponseEntity<Void> deleteUser(Integer id) {
        userService.deleteById(id.longValue());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<DeckDTO>> getUserDecks(Integer id, Integer pagesize, Integer pagenumber) {
        var decks = userService.getUserDecks(
            id.longValue(),
            pagesize != null ? pagesize : 10,
            pagenumber != null ? pagenumber : 0
        );
        return ResponseEntity.ok(deckMapper.toDecksDTO(decks));
    }
}