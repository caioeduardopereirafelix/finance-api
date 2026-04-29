package io.github.caioeduardopereirafelix.finance_api.model.mapper;

import io.github.caioeduardopereirafelix.finance_api.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.finance_api.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {


    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "email", target = "email")
    @Mapping(target = "password", ignore = true)
    User toUser(UserDTO dto);


}
