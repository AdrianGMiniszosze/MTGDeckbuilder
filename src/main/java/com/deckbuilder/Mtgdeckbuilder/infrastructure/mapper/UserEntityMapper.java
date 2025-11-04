package com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.UserEntity;
import com.deckbuilder.Mtgdeckbuilder.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    
    @Mapping(target = "registrationDate", ignore = true)
    UserEntity toEntity(User model);
    
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    User toModel(UserEntity entity);
    
    List<UserEntity> toEntityList(List<User> models);
    
    List<User> toModelList(List<UserEntity> entities);
}
