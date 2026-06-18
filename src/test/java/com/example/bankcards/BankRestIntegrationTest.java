package com.example.bankcards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.user.CreateUserRequest;
import com.example.bankcards.entity.enums.Role;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class BankRestIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private FilterChainProxy springSecurityFilterChain;
    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private MockMvc mvc() {
        if (mockMvc == null) {
            mockMvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(springSecurityFilterChain)
                    .build();
        }
        return mockMvc;
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    private long createCardForUser2(String adminToken, String number, BigDecimal balance) throws Exception {
        CreateCardRequest request = new CreateCardRequest(number, 2L, LocalDate.now().plusYears(3), balance);
        MvcResult result = mvc().perform(post("/api/v1/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedNumber", is("**** **** **** " + number.substring(number.length() - 4))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc().perform(get("/api/v1/cards/my")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessAdminEndpoints() throws Exception {
        String userToken = login("user", "User12345");
        mvc().perform(get("/api/v1/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageUsers() throws Exception {
        String adminToken = login("admin", "Admin12345");
        CreateUserRequest request =
                new CreateUserRequest("created_" + System.nanoTime(), "Password1", "Created", Role.USER);

        mvc().perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void invalidCredentialsReturn401() throws Exception {
        mvc().perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorReturns400WithFieldErrors() throws Exception {
        String adminToken = login("admin", "Admin12345");
        mvc().perform(post("/api/v1/cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\":\"abc\",\"ownerId\":2,\"expiryDate\":\"2030-12-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void fullCardAndTransferFlow() throws Exception {
        String adminToken = login("admin", "Admin12345");
        long fromId = createCardForUser2(adminToken, "4111111111111111", new BigDecimal("1000.00"));
        long toId = createCardForUser2(adminToken, "4222222222222222", new BigDecimal("0.00"));

        String userToken = login("user", "User12345");

        mvc().perform(get("/api/v1/cards/my").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        TransferRequest transfer = new TransferRequest(fromId, toId, new BigDecimal("250.00"));
        mvc().perform(post("/api/v1/cards/my/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isNoContent());

        assertBalance(userToken, fromId, "750.0");
        assertBalance(userToken, toId, "250.0");

        TransferRequest tooMuch = new TransferRequest(fromId, toId, new BigDecimal("100000.00"));
        mvc().perform(post("/api/v1/cards/my/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooMuch)))
                .andExpect(status().isUnprocessableContent());
    }

    private void assertBalance(String token, long cardId, String expected) throws Exception {
        MvcResult result = mvc().perform(get("/api/v1/cards/my/" + cardId + "/balance")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(new BigDecimal(node.get("balance").asString())).isEqualByComparingTo(expected);
    }
}
