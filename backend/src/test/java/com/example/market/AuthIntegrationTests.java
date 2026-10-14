package com.example.market;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.market.common.SecurityConfiguration;
import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实PostgreSQL与真实安全过滤链；事务回滚，不打印含密码的请求或JWT。 */
@SpringBootTest(classes = com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@Transactional
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    private static final String PASSWORD = "AuthTest_2026!";

    @Test
    void registrationLoginAndCurrentUserWorkWithRealDatabase() throws Exception {
        String username = uniqueName();
        register(username);
        User stored = find(username);
        assertNotNull(stored);
        assertNotEquals(PASSWORD, stored.getPassword());
        assertTrue(stored.getPassword().startsWith("$2"));
        assertTrue(passwords.matches(PASSWORD, stored.getPassword()));
        assertEquals("USER", stored.getRole());
        assertEquals("ACTIVE", stored.getStatus());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", PASSWORD, "nickname", "重复用户"))))
                .andExpect(status().isConflict());
        String token = login(username);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("username").value(username))
                .andExpect(jsonPath("password").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", "IncorrectPassword!"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid.jwt.value"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health")).andExpect(status().isOk());

        Instant now = Instant.now();
        var expiredClaims = JwtClaimsSet.builder().issuer(SecurityConfiguration.ISSUER)
                .subject(stored.getId().toString()).issuedAt(now.minusSeconds(1200))
                .expiresAt(now.minusSeconds(600)).build();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), expiredClaims)).getTokenValue();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountCannotLoginOrUseExistingToken() throws Exception {
        String username = uniqueName();
        register(username);
        String token = login(username);
        User user = find(username);
        user.setStatus("DISABLED");
        users.updateById(user);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRegistrationAndOversizedUtf8PasswordAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", "a", "password", PASSWORD, "nickname", "用户"))))
                .andExpect(status().isBadRequest());
        String username = uniqueName();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", "密".repeat(30), "nickname", "用户"))))
                .andExpect(status().isBadRequest());
        assertNull(find(username));
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", PASSWORD, "nickname", "测试用户", "role", "ADMIN"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("role").value("USER"))
                .andExpect(jsonPath("password").doesNotExist());
    }

    private String login(String username) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("username", username, "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("token").isString())
                .andExpect(jsonPath("user.username").value(username))
                .andExpect(jsonPath("user.password").doesNotExist()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();
        assertEquals(3, token.split("\\.").length);
        return token;
    }

    private User find(String username) {
        return users.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
    private String body(Map<String, String> values) throws Exception { return json.writeValueAsString(values); }
    private String uniqueName() { return "auth_" + UUID.randomUUID().toString().replace("-", ""); }
}
