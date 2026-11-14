package com.deckbuilder.mtgdeckbuilder.contract.mapper;

import com.deckbuilder.apigenerator.openapi.api.model.UserDTO;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
	@Mapping(source = "hashedPassword", target = "hashed_password")
	@Mapping(source = "registrationDate", target = "registration_date")
	UserDTO toUserDTO(User user);

	List<UserDTO> toUserDTOs(List<User> users);

	@Mapping(source = "hashed_password", target = "hashedPassword")
	@Mapping(source = "registration_date", target = "registrationDate")
	@Mapping(target = "isActive", ignore = true)
	@Mapping(target = "lastLogin", ignore = true)
	User toUser(UserDTO userDTO);

	@Mapping(source = "hashed_password", target = "hashedPassword")
	@Mapping(source = "registration_date", target = "registrationDate")
	@Mapping(target = "isActive", ignore = true)
	@Mapping(target = "lastLogin", ignore = true)
	void updateUser(@MappingTarget User user, UserDTO userDTO);
}