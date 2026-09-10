package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.user.ResponseUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserController controller;

    @Test
    void putUserDeveResolverOPathVariableDaRota() throws Exception {
        var id = UUID.randomUUID();
        var user = User.builder().id(id).name("Caio").email("caio@test.com").build();

        when(userService.updateUser(eq(id), any(UpdateUserDTO.class))).thenReturn(user);
        when(mapper.toUserResponse(user))
                .thenReturn(new ResponseUserDTO(id.toString(), "Caio", "caio@test.com"));

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(put("/user/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Caio\",\"email\":\"caio@test.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("caio@test.com"));
    }
}
