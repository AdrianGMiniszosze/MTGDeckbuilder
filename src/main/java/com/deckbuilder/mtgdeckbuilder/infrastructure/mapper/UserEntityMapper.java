package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.mtgdeckbuilder.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "role", defaultValue = "USER")
	@Mapping(target = "isActive", defaultValue = "true")
	@Mapping(target = "lastLogin", ignore = true)
	UserEntity toEntity(User model);

	@Mapping(source = "createdAt", target = "registrationDate")
	User toModel(UserEntity entity);

	List<UserEntity> toEntityList(List<User> models);

	List<User> toModelList(List<UserEntity> entities);
}
