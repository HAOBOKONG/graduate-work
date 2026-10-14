package com.example.market;

import com.example.market.entity.Product;
import com.example.market.entity.ProductImage;
import com.example.market.entity.User;
import com.example.market.mapper.ProductImageMapper;
import com.example.market.mapper.ProductMapper;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@Transactional
class FavoriteIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired ProductMapper products;
    @Autowired ProductImageMapper images;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;

    @Test void favoriteListOwnershipDuplicateAndCancel() throws Exception {
        User owner = user(), other = user();
        Product product = product(owner);
        String token = "Bearer " + tokens.issue(owner);
        String otherToken = "Bearer " + tokens.issue(other);

        mvc.perform(post("/api/favorites/" + product.getId())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/favorites/my")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/favorites/" + product.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/favorites/" + Long.MAX_VALUE).header("Authorization", token))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/favorites/" + product.getId()).param("userId", String.valueOf(other.getId()))
            .header("Authorization", token)).andExpect(status().isCreated())
            .andExpect(jsonPath("message").value("收藏成功"));
        assertEquals(owner.getId(), jdbc.queryForObject(
            "SELECT user_id FROM favorites WHERE product_id=?", Long.class, product.getId()));
        mvc.perform(post("/api/favorites/" + product.getId()).header("Authorization", token))
            .andExpect(status().isConflict()).andExpect(jsonPath("code").value(409));
        mvc.perform(get("/api/favorites/my").header("Authorization", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(product.getId()))
            .andExpect(jsonPath("$[0].title").value(product.getTitle()))
            .andExpect(jsonPath("$[0].price").value(39.90))
            .andExpect(jsonPath("$[0].image").value("https://example.com/favorite.jpg"))
            .andExpect(jsonPath("$[0].createdAt").exists());
        mvc.perform(get("/api/favorites/my").header("Authorization", otherToken))
            .andExpect(jsonPath("$").isEmpty());
        mvc.perform(delete("/api/favorites/" + product.getId()).header("Authorization", otherToken))
            .andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM favorites WHERE product_id=?", Integer.class, product.getId()));
        mvc.perform(delete("/api/favorites/" + product.getId()).header("Authorization", token))
            .andExpect(status().isNoContent());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM favorites WHERE product_id=?", Integer.class, product.getId()));
        mvc.perform(get("/api/favorites/my").header("Authorization", token))
            .andExpect(jsonPath("$").isEmpty());
    }

    private User user() {
        User user = new User();
        user.setUsername("fav_" + UUID.randomUUID().toString().replace("-", ""));
        user.setNickname("收藏测试用户");
        user.setPassword(passwords.encode("FavoriteTest_2026!"));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        users.insert(user);
        return user;
    }

    private Product product(User seller) {
        Product product = new Product();
        product.setSellerId(seller.getId());
        product.setCategoryId(jdbc.queryForObject("SELECT min(id) FROM categories", Long.class));
        product.setTitle("收藏测试商品");
        product.setDescription("收藏流程测试");
        product.setPrice(new BigDecimal("39.90"));
        product.setConditionLevel(4);
        product.setStatus("ON_SALE");
        products.insert(product);
        ProductImage image = new ProductImage();
        image.setProductId(product.getId());
        image.setImageUrl("https://example.com/favorite.jpg");
        image.setSortOrder(0);
        images.insert(image);
        return product;
    }
}
