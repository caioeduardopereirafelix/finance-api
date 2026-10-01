package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected record Account(String email, String password, String accessToken, String refreshToken) {

        String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @Autowired
    protected UserRepository userRepository;

    protected Account registerAndLogin() throws Exception {
        var account = registerAndLoginUnverified();
        var user = userRepository.findByEmail(account.email()).orElseThrow();
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
        return account;
    }

    protected Account registerAndLoginUnverified() throws Exception {

        String email = "user-" + UUID.randomUUID() + "@test.com";
        String password = "senha-segura-1";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","user":"Usuario Teste","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isCreated());

        return new Account(email, password,
                loginNode(email, password).get("token").asText(),
                loginNode(email, password).get("refreshToken").asText());
    }

    protected JsonNode loginNode(String email, String password) throws Exception {

        String body = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body);
    }
}
