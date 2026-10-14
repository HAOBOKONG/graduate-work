package com.example.market;

import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import com.example.market.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** 真实PostgreSQL、JWT和安全过滤链，验证后台权限与审核。 */
@SpringBootTest(classes=com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS", matches="true")
class AdminIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;
    @Autowired OrderService orders;

    @Test @Transactional
    void entireAdminNamespaceRequiresCurrentAdminRole() throws Exception {
        User ordinary=user(false), admin=user(true);
        String ordinaryToken=tokens.issue(ordinary), adminToken=tokens.issue(admin);
        for(String path:List.of("/api/admin/products","/api/admin/products/pending","/api/admin/users","/api/admin/unknown")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization","Bearer "+ordinaryToken)).andExpect(status().isForbidden());
        }
        for(String path:List.of("/api/admin/products/1/approve","/api/admin/products/1/reject","/api/admin/users/1/disable","/api/admin/users/1/enable")) {
            mvc.perform(patch(path).header("Authorization","Bearer "+ordinaryToken)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/admin/products/1").header("Authorization","Bearer "+ordinaryToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",admin.getUsername(),"password","AdminTest_2026!"))))
            .andExpect(status().isOk()).andExpect(jsonPath("user.role").value("ADMIN"));
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        admin.setRole("USER"); users.updateById(admin);
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+adminToken)).andExpect(status().isForbidden());
        admin.setRole("ADMIN"); admin.setStatus("DISABLED"); users.updateById(admin);
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+adminToken)).andExpect(status().isUnauthorized());
    }

    @Test @Transactional
    void reviewAndEditedProductCannotBypassApproval() throws Exception {
        User seller=user(false), admin=user(true);
        String st=tokens.issue(seller), at=tokens.issue(admin);
        Map<String,Object> data=productBody(); data.put("status","ON_SALE");
        JsonNode created=publish(st,data); long id=created.path("product").path("id").asLong();
        assertEquals("PENDING_REVIEW",created.path("product").path("status").asText());
        mvc.perform(get("/api/products").param("keyword",(String)data.get("title"))).andExpect(jsonPath("total").value(0));
        mvc.perform(get("/api/admin/products/pending").header("Authorization","Bearer "+at))
            .andExpect(status().isOk()).andExpect(jsonPath("records[0].id").value(id));
        mvc.perform(patch("/api/admin/products/"+id+"/approve").header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        assertEquals("ON_SALE",state(id));
        mvc.perform(get("/api/products").param("keyword",(String)data.get("title"))).andExpect(jsonPath("total").value(1));
        mvc.perform(patch("/api/admin/products/"+id+"/reject").header("Authorization","Bearer "+at)).andExpect(status().isConflict());
        mvc.perform(put("/api/products/"+id).header("Authorization","Bearer "+st).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(data))).andExpect(status().isOk()).andExpect(jsonPath("product.status").value("PENDING_REVIEW"));
        mvc.perform(patch("/api/admin/products/"+id+"/reject").header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        assertEquals("REJECTED",state(id));
        mvc.perform(get("/api/products").param("keyword",(String)data.get("title"))).andExpect(jsonPath("total").value(0));
        mvc.perform(put("/api/products/"+id).header("Authorization","Bearer "+st).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(data))).andExpect(jsonPath("product.status").value("PENDING_REVIEW"));
    }

    @Test @Transactional
    void disableAndRestoreTakeEffectOnExistingTokensAndAdminIsProtected() throws Exception {
        User admin=user(true), ordinary=user(false); String at=tokens.issue(admin), ut=tokens.issue(ordinary);
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+at).param("size","100"))
            .andExpect(status().isOk()).andExpect(jsonPath("records[0].password").doesNotExist());
        mvc.perform(patch("/api/admin/users/"+ordinary.getId()+"/disable").header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        assertEquals("DISABLED",users.selectById(ordinary.getId()).getStatus());
        for(String path:List.of("/api/users/me","/api/products/my","/api/orders/my","/api/products")) {
            mvc.perform(get(path).header("Authorization","Bearer "+ut)).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/products").header("Authorization","Bearer "+ut).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(productBody()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+ut).contentType(MediaType.APPLICATION_JSON)
            .content("{\"productId\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("username",ordinary.getUsername(),"password","AdminTest_2026!")))).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/"+ordinary.getId()+"/enable").header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        mvc.perform(get("/api/orders/my").header("Authorization","Bearer "+ut)).andExpect(status().isOk());
        for(String action:List.of("disable","enable")) {
            mvc.perform(patch("/api/admin/users/"+admin.getId()+"/"+action).header("Authorization","Bearer "+at)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/admin/users/"+admin.getId()).header("Authorization","Bearer "+at)).andExpect(status().isNotFound());
        assertEquals("ACTIVE",users.selectById(admin.getId()).getStatus());
    }

    @Test @Transactional
    void deletionPaginationAndTransactionsAreProtected() throws Exception {
        User admin=user(true), seller=user(false), buyer=user(false); String at=tokens.issue(admin), st=tokens.issue(seller);
        long id=publish(st,productBody()).path("product").path("id").asLong();
        mvc.perform(get("/api/admin/products").header("Authorization","Bearer "+at).param("size","1"))
            .andExpect(status().isOk()).andExpect(jsonPath("records.length()").value(1));
        mvc.perform(delete("/api/admin/products/"+id).header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM products WHERE id=?",Integer.class,id));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM product_images WHERE product_id=?",Integer.class,id));
        mvc.perform(delete("/api/admin/products/"+id).header("Authorization","Bearer "+at)).andExpect(status().isNotFound());
        long traded=publish(st,productBody()).path("product").path("id").asLong();
        mvc.perform(patch("/api/admin/products/"+traded+"/approve").header("Authorization","Bearer "+at)).andExpect(status().isNoContent());
        orders.create(traded,buyer.getId());
        mvc.perform(delete("/api/admin/products/"+traded).header("Authorization","Bearer "+at)).andExpect(status().isConflict());
        assertEquals("RESERVED",state(traded));
        for(String path:List.of("/api/admin/products","/api/admin/products/pending","/api/admin/users")) {
            mvc.perform(get(path).header("Authorization","Bearer "+at).param("page","0")).andExpect(status().isBadRequest());
            mvc.perform(get(path).header("Authorization","Bearer "+at).param("size","101")).andExpect(status().isBadRequest());
        }
    }

    @Test
    void foreignKeyFailureRollsBackPictureDeletion() throws Exception {
        User admin=user(true), seller=user(false); String at=tokens.issue(admin);
        long id=publish(tokens.issue(seller),productBody()).path("product").path("id").asLong();
        try {
            jdbc.update("INSERT INTO reports(reporter_id,product_id,reason) VALUES(?,?,?)",seller.getId(),id,"仅测试关联保护");
            mvc.perform(delete("/api/admin/products/"+id).header("Authorization","Bearer "+at)).andExpect(status().isConflict());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM product_images WHERE product_id=?",Integer.class,id));
            assertEquals("PENDING_REVIEW",state(id));
        } finally {
            jdbc.update("DELETE FROM reports WHERE product_id=?",id);
            jdbc.update("DELETE FROM product_images WHERE product_id=?",id);
            jdbc.update("DELETE FROM products WHERE id=?",id);
            users.deleteById(seller.getId()); users.deleteById(admin.getId());
        }
    }
    private String state(long id) { return jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,id); }
    private JsonNode publish(String token,Map<String,Object> body) throws Exception {
        String response=mvc.perform(post("/api/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
    private Map<String,Object> productBody() {
        Map<String,Object> body=new HashMap<>(); body.put("title","审核测试_"+UUID.randomUUID()); body.put("description","真实审核流程测试");
        body.put("categoryId",jdbc.queryForObject("SELECT min(id) FROM categories",Long.class)); body.put("price","49.90");
        body.put("conditionLevel",4); body.put("imageUrls",List.of("https://example.com/review.jpg")); return body;
    }
    private User user(boolean admin) {
        User user=new User(); user.setUsername("adm_"+UUID.randomUUID().toString().replace("-","")); user.setNickname("后台测试用户");
        user.setPassword(passwords.encode("AdminTest_2026!")); user.setRole(admin?"ADMIN":"USER"); user.setStatus("ACTIVE"); users.insert(user); return user;
    }
}
