package com.example.market;

import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实数据库中的完整交易演示；写入在测试结束时回滚。 */
@SpringBootTest(classes = com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@Transactional
class FullBusinessFlowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;

    @Test void registrationReviewPurchaseAndReceipt() throws Exception {
        String sellerName = "flow_a_" + UUID.randomUUID().toString().replace("-", "");
        String buyerName = "flow_b_" + UUID.randomUUID().toString().replace("-", "");
        String seller = registerAndLogin(sellerName);
        String buyer = registerAndLogin(buyerName);
        User administrator = new User();
        administrator.setUsername("flow_admin_" + UUID.randomUUID().toString().replace("-", ""));
        administrator.setNickname("流程管理员");
        administrator.setPassword(passwords.encode("TestPass_2026!"));
        administrator.setRole("ADMIN");
        administrator.setStatus("ACTIVE");
        users.insert(administrator);
        String admin = tokens.issue(administrator);

        long category = jdbc.queryForObject("SELECT min(id) FROM categories", Long.class);
        String title = "完整流程_" + UUID.randomUUID();
        long product = response(post("/api/products").header("Authorization", bearer(seller))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "title", title, "description", "毕业设计流程测试", "categoryId", category,
                "price", "88.50", "conditionLevel", 4, "imageUrls", new String[0]))))
            .path("product").path("id").asLong();
        assertEquals("PENDING_REVIEW", productStatus(product));
        mvc.perform(get("/api/products").param("keyword", title)).andExpect(jsonPath("total").value(0));
        mvc.perform(patch("/api/admin/products/" + product + "/approve").header("Authorization", bearer(buyer)))
            .andExpect(status().isForbidden()).andExpect(jsonPath("code").value(403));
        mvc.perform(patch("/api/admin/products/" + product + "/approve").header("Authorization", bearer(admin)))
            .andExpect(status().isNoContent());
        assertEquals("ON_SALE", productStatus(product));
        mvc.perform(get("/api/products").param("keyword", "毕业设计流程测试")
            .param("categoryId", String.valueOf(category))).andExpect(jsonPath("records[0].id").value(product));

        long order = response(post("/api/orders").header("Authorization", bearer(buyer))
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("productId", product))))
            .path("id").asLong();
        assertEquals("RESERVED", productStatus(product));
        assertEquals("WAIT_PAY", orderStatus(order));
        mvc.perform(get("/api/orders/my").header("Authorization", bearer(seller)))
            .andExpect(jsonPath("sales[0].id").value(order));
        mvc.perform(patch("/api/orders/" + order + "/pay").header("Authorization", bearer(buyer)))
            .andExpect(jsonPath("status").value("PAID"));
        mvc.perform(patch("/api/orders/" + order + "/ship").header("Authorization", bearer(seller)))
            .andExpect(jsonPath("status").value("SHIPPED"));
        mvc.perform(patch("/api/orders/" + order + "/complete").header("Authorization", bearer(buyer)))
            .andExpect(jsonPath("status").value("COMPLETED"));
        assertEquals("COMPLETED", orderStatus(order));
        assertEquals("SOLD", productStatus(product));
        assertEquals("88.50", jdbc.queryForObject("SELECT price_snapshot::text FROM orders WHERE id=?", String.class, order));
    }

    private String registerAndLogin(String username) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", username, "password", "TestPass_2026!", "nickname", username))))
            .andExpect(status().isCreated());
        return response(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username", username, "password", "TestPass_2026!"))))
            .path("token").asText();
    }

    private JsonNode response(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is2xxSuccessful())
            .andReturn().getResponse().getContentAsString());
    }
    private String bearer(String token) { return "Bearer " + token; }
    private String productStatus(long id) { return jdbc.queryForObject("SELECT status FROM products WHERE id=?", String.class, id); }
    private String orderStatus(long id) { return jdbc.queryForObject("SELECT status FROM orders WHERE id=?", String.class, id); }
}
