package com.example.market;

import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import com.example.market.service.AdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 真实PostgreSQL与安全过滤链；测试业务写入随事务回滚。 */
@SpringBootTest(classes=com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS", matches="true")
@Transactional
class ProductIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;
    @Autowired AdminService admin;

    @Test void publishListDetailEditAndOffShelfPersistInPostgres() throws Exception {
        User seller=user(); String token=tokens.issue(seller);
        Map<String,Object> body=request();
        body.put("sellerId",999999); body.put("status","SOLD");
        String title=(String)body.get("title");
        JsonNode created=json.readTree(mvc.perform(post("/api/products").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andExpect(jsonPath("product.status").value("PENDING_REVIEW"))
            .andExpect(jsonPath("product.sellerId").value(seller.getId()))
            .andExpect(jsonPath("product.images[0]").value("https://example.com/test.jpg"))
            .andReturn().getResponse().getContentAsString());
        long id=created.path("product").path("id").asLong();
        mvc.perform(get("/api/products").param("keyword",title)).andExpect(jsonPath("total").value(0));
        admin.review(id,true);
        assertEquals(new BigDecimal("59.90"),jdbc.queryForObject("SELECT price FROM products WHERE id=?",BigDecimal.class,id));
        assertEquals(seller.getId(),jdbc.queryForObject("SELECT seller_id FROM products WHERE id=?",Long.class,id));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM product_images WHERE product_id=?",Integer.class,id));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").isString());
        mvc.perform(get("/api/products").param("keyword",title).param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("total").value(1)).andExpect(jsonPath("records[0].id").value(id))
            .andExpect(jsonPath("records[0].image").value("https://example.com/test.jpg"));
        mvc.perform(get("/api/products").param("keyword","真实数据库测试商品")
            .param("categoryId",String.valueOf(body.get("categoryId"))))
            .andExpect(status().isOk()).andExpect(jsonPath("records[0].id").value(id));
        mvc.perform(get("/api/products").param("keyword",title)
            .param("categoryId",String.valueOf(Long.MAX_VALUE)))
            .andExpect(jsonPath("total").value(0));
        mvc.perform(get("/api/products").param("keyword",title).param("page","2").param("size","1"))
            .andExpect(jsonPath("records").isEmpty()).andExpect(jsonPath("total").value(1));
        mvc.perform(get("/api/products/"+id)).andExpect(status().isOk())
            .andExpect(jsonPath("seller.nickname").value("商品测试用户"))
            .andExpect(jsonPath("seller.password").doesNotExist()).andExpect(jsonPath("category.name").isString());
        mvc.perform(get("/api/products/my").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("records[0].id").value(id));
        body.put("title",title+"修改"); body.put("originalPrice",null); body.put("imageUrls",List.of());
        mvc.perform(put("/api/products/"+id).header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isOk()).andExpect(jsonPath("product.title").value(title+"修改"))
            .andExpect(jsonPath("product.originalPrice").isEmpty()).andExpect(jsonPath("product.images").isEmpty());
        assertNull(jdbc.queryForObject("SELECT original_price FROM products WHERE id=?",BigDecimal.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM product_images WHERE product_id=?",Integer.class,id));
        assertEquals("PENDING_REVIEW",jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,id));
        admin.review(id,true);
        mvc.perform(patch("/api/products/"+id+"/off-shelf").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("product.status").value("OFF_SHELF"));
        assertEquals("OFF_SHELF",jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,id));
        mvc.perform(get("/api/products").param("keyword",title)).andExpect(jsonPath("total").value(0));
        mvc.perform(get("/api/products/my").header("Authorization","Bearer "+token))
            .andExpect(jsonPath("records[0].status").value("OFF_SHELF"));
    }

    @Test void anonymousOtherUsersAndDisabledAccountsCannotManageProducts() throws Exception {
        User owner=user(), other=user(); String token=tokens.issue(owner), otherToken=tokens.issue(other);
        Map<String,Object> body=request(); String payload=json.writeValueAsString(body);
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products/my")).andExpect(status().isUnauthorized());
        long id=json.readTree(mvc.perform(post("/api/products").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(payload)).andReturn().getResponse().getContentAsString()).path("product").path("id").asLong();
        admin.review(id,true);
        mvc.perform(put("/api/products/"+id).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/products/"+id+"/off-shelf")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/products/"+id).header("Authorization","Bearer "+otherToken)
            .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/products/"+id+"/off-shelf").header("Authorization","Bearer "+otherToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/products/my").header("Authorization","Bearer "+otherToken)).andExpect(jsonPath("total").value(0));
        assertEquals("ON_SALE",jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,id));
        owner.setStatus("DISABLED"); users.updateById(owner);
        mvc.perform(patch("/api/products/"+id+"/off-shelf").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
    }

    @Test void invalidFieldsPaginationAndUnknownProductsAreRejected() throws Exception {
        String token=tokens.issue(user());
        for (Map.Entry<String,Object> invalid:Map.<String,Object>of("price","0","conditionLevel",6,"categoryId",Long.MAX_VALUE,
                "title"," ","imageUrls",List.of("javascript:alert(1)")).entrySet()) {
            Map<String,Object> body=request(); body.put(invalid.getKey(),invalid.getValue());
            mvc.perform(post("/api/products").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        }
        Map<String,Object> body=request(); body.put("price","1.234");
        mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page","0")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value(400)).andExpect(jsonPath("data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/products").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("categoryId","abc")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value(400));
        mvc.perform(get("/api/products/"+Long.MAX_VALUE)).andExpect(status().isNotFound());
    }
    private Map<String,Object> request() {
        Map<String,Object> body=new HashMap<>();
        body.put("title","商品_"+UUID.randomUUID()); body.put("description","真实数据库测试商品");
        body.put("categoryId",jdbc.queryForObject("SELECT min(id) FROM categories",Long.class));
        body.put("price","59.90"); body.put("originalPrice","120.00"); body.put("conditionLevel",4);
        body.put("imageUrls",List.of("https://example.com/test.jpg")); return body;
    }
    private User user() {
        User user=new User(); user.setUsername("product_"+UUID.randomUUID().toString().replace("-",""));
        user.setNickname("商品测试用户"); user.setPassword(passwords.encode("ProductTest_2026!"));
        user.setRole("USER"); user.setStatus("ACTIVE"); users.insert(user); return user;
    }
}
