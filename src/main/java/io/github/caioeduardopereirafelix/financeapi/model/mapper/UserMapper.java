package io.github.caioeduardopereirafelix.financeapi.model.mapper;

import io.github.caioeduardopereirafelix.financeapi.model.dto.user.CreateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.ResponseUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(source = "name", target = "name")
    @Mapping(source = "email", target = "email")
    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserDTO dto);

    CreateUserDTO toUserDto(User user);


    ResponseUserDTO toUserResponse (User dto);
}
