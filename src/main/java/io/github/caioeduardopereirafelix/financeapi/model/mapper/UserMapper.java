package io.github.caioeduardopereirafelix.financeapi.model.mapper;

import io.github.caioeduardopereirafelix.financeapi.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(source = "name", target = "name")
    @Mapping(source = "email", target = "email")
    @Mapping(target = "password", ignore = true)
    User toUser(UserDTO dto);

    UserDTO toUserDto(User user);


}
