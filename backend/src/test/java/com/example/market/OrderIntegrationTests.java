package com.example.market;

import com.example.market.entity.Product;
import com.example.market.entity.User;
import com.example.market.mapper.ProductMapper;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import com.example.market.service.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=com.example.secondhandmarket.SecondhandMarketApplication.class)
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS", matches="true")
class OrderIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired ProductMapper products;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;
    @Autowired OrderService orders;
    @Autowired PlatformTransactionManager transactions;

    @Test @Transactional
    void realPurchasePaymentShippingAndReceiptCompleteTransaction() throws Exception {
        User seller=user(), buyer=user(); String sellerToken=tokens.issue(seller), buyerToken=tokens.issue(buyer);
        Product product=product(seller);
        JsonNode created=create(product.getId(),buyerToken,Map.of("buyerId",seller.getId(),"sellerId",buyer.getId(),"priceSnapshot","0.01","status","COMPLETED"));
        long id=created.path("id").asLong();
        assertEquals("WAIT_PAY",created.path("status").asText());
        assertEquals(buyer.getId().longValue(),created.path("buyerId").asLong());
        assertEquals(seller.getId().longValue(),created.path("sellerId").asLong());
        assertEquals(product.getTitle(),created.path("productTitleSnapshot").asText());
        assertEquals(new BigDecimal("78.90"),jdbc.queryForObject("SELECT price_snapshot FROM orders WHERE id=?",BigDecimal.class,id));
        assertEquals("RESERVED",productStatus(product));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders WHERE order_no=?",Integer.class,created.path("orderNo").asText()));
        mvc.perform(get("/api/orders/my").header("Authorization","Bearer "+buyerToken))
            .andExpect(jsonPath("purchases[0].id").value(id)).andExpect(jsonPath("sales").isEmpty());
        mvc.perform(get("/api/orders/my").header("Authorization","Bearer "+sellerToken))
            .andExpect(jsonPath("sales[0].id").value(id)).andExpect(jsonPath("purchases").isEmpty());
        mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+buyerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("buyer.password").doesNotExist());
        mvc.perform(patch("/api/orders/"+id+"/pay").header("Authorization","Bearer "+buyerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("PAID"));
        mvc.perform(patch("/api/orders/"+id+"/ship").header("Authorization","Bearer "+sellerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("SHIPPED"));
        mvc.perform(patch("/api/orders/"+id+"/complete").header("Authorization","Bearer "+buyerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("COMPLETED"));
        assertEquals("COMPLETED",jdbc.queryForObject("SELECT status FROM orders WHERE id=?",String.class,id));
        assertEquals("SOLD",productStatus(product));
        // 模拟后续商品信息改变，订单详情仍返回成交快照。
        jdbc.update("UPDATE products SET title=?,price=? WHERE id=?","后续商品标题",new BigDecimal("99.00"),product.getId());
        mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+buyerToken))
            .andExpect(jsonPath("productTitleSnapshot").value(product.getTitle())).andExpect(jsonPath("priceSnapshot").value(78.90));
    }

    @Test @Transactional
    void authenticationOwnershipAndActorRolesAreEnforced() throws Exception {
        User seller=user(), buyer=user(), stranger=user(); Product product=product(seller);
        String st=tokens.issue(seller), bt=tokens.issue(buyer), xt=tokens.issue(stranger);
        String payload=json.writeValueAsString(Map.of("productId",product.getId()));
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+st).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest());
        assertEquals("ON_SALE",productStatus(product));
        long id=create(product.getId(),bt,Map.of()).path("id").asLong();
        mvc.perform(get("/api/orders/"+id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+xt)).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/my").header("Authorization","Bearer "+xt)).andExpect(jsonPath("purchases").isEmpty()).andExpect(jsonPath("sales").isEmpty());
        for(String action:List.of("pay","ship","complete")) {
            mvc.perform(patch("/api/orders/"+id+"/"+action)).andExpect(status().isUnauthorized());
            mvc.perform(patch("/api/orders/"+id+"/"+action).header("Authorization","Bearer "+xt)).andExpect(status().isForbidden());
        }
        mvc.perform(patch("/api/orders/"+id+"/pay").header("Authorization","Bearer "+st)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/"+id+"/ship").header("Authorization","Bearer "+bt)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/"+id+"/complete").header("Authorization","Bearer "+st)).andExpect(status().isForbidden());
        buyer.setStatus("DISABLED"); users.updateById(buyer);
        mvc.perform(patch("/api/orders/"+id+"/pay").header("Authorization","Bearer "+bt)).andExpect(status().isUnauthorized());
        assertEquals("WAIT_PAY",jdbc.queryForObject("SELECT status FROM orders WHERE id=?",String.class,id));
    }

    @Test @Transactional
    void invalidTransitionsRepeatedPurchaseAndReservedEditsAreRejected() throws Exception {
        User seller=user(), buyer=user(); Product product=product(seller);
        String st=tokens.issue(seller), bt=tokens.issue(buyer);
        long id=create(product.getId(),bt,Map.of()).path("id").asLong();
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+bt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("productId",product.getId())))).andExpect(status().isConflict());
        mvc.perform(patch("/api/products/"+product.getId()+"/off-shelf").header("Authorization","Bearer "+st)).andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/"+id+"/ship").header("Authorization","Bearer "+st)).andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/"+id+"/complete").header("Authorization","Bearer "+bt)).andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/"+id+"/pay").header("Authorization","Bearer "+bt)).andExpect(status().isOk());
        mvc.perform(patch("/api/orders/"+id+"/pay").header("Authorization","Bearer "+bt)).andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/"+id+"/ship").header("Authorization","Bearer "+st)).andExpect(status().isOk());
        mvc.perform(patch("/api/orders/"+id+"/ship").header("Authorization","Bearer "+st)).andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/"+id+"/complete").header("Authorization","Bearer "+bt)).andExpect(status().isOk());
        mvc.perform(patch("/api/orders/"+id+"/complete").header("Authorization","Bearer "+bt)).andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders WHERE product_id=?",Integer.class,product.getId()));
        Product off=product(seller); off.setStatus("OFF_SHELF"); products.updateById(off);
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+bt).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("productId",off.getId())))).andExpect(status().isConflict());
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+bt).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders/"+Long.MAX_VALUE).header("Authorization","Bearer "+bt)).andExpect(status().isNotFound());
    }

    @Test
    void concurrentBuyersCreateExactlyOneCommittedOrder() throws Exception {
        TransactionTemplate tx=new TransactionTemplate(transactions);
        List<User> people=new ArrayList<>();
        Product product=tx.execute(status -> { User seller=user(), a=user(), b=user(); people.addAll(List.of(seller,a,b)); return product(seller); });
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(int i=1;i<=2;i++) {
                long buyerId=people.get(i).getId();
                results.add(pool.submit(() -> {
                    if(!start.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                    try { orders.create(product.getId(),buyerId); return true; }
                    catch(ResponseStatusException failure) { assertEquals(409,failure.getStatusCode().value()); return false; }
                }));
            }
            start.countDown();
            int successes=0;
            for(Future<Boolean> result:results) if(result.get(20,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM orders WHERE product_id=?",Integer.class,product.getId()));
            assertEquals("RESERVED",productStatus(product));
        } finally {
            pool.shutdownNow(); pool.awaitTermination(10,TimeUnit.SECONDS);
            // 只删除本测试创建的临时记录，不触及任何已有业务数据。
            tx.executeWithoutResult(status -> {
                jdbc.update("DELETE FROM orders WHERE product_id=?",product.getId());
                products.deleteById(product.getId()); people.forEach(person -> users.deleteById(person.getId()));
            });
        }
    }

    @Test
    void failedOrderInsertRollsBackProductReservation() {
        TransactionTemplate tx=new TransactionTemplate(transactions);
        User seller=tx.execute(status -> user());
        Product product=tx.execute(status -> product(seller));
        try {
            // 真实外键失败；必须在服务事务结束后观察商品仍为ON_SALE。
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,() -> orders.create(product.getId(),Long.MAX_VALUE));
            assertEquals("ON_SALE",productStatus(product));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM orders WHERE product_id=?",Integer.class,product.getId()));
        } finally {
            tx.executeWithoutResult(status -> { products.deleteById(product.getId()); users.deleteById(seller.getId()); });
        }
    }
    private JsonNode create(Long productId,String token,Map<String,Object> extras) throws Exception {
        Map<String,Object> body=new HashMap<>(extras); body.put("productId",productId);
        String response=mvc.perform(post("/api/orders").header("Authorization","Bearer "+token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
    private String productStatus(Product product) { return jdbc.queryForObject("SELECT status FROM products WHERE id=?",String.class,product.getId()); }
    private User user() {
        User user=new User(); user.setUsername("order_"+UUID.randomUUID().toString().replace("-","")); user.setNickname("订单测试用户");
        user.setPassword(passwords.encode("OrderTest_2026!")); user.setRole("USER"); user.setStatus("ACTIVE"); users.insert(user); return user;
    }
    private Product product(User seller) {
        Product p=new Product(); p.setSellerId(seller.getId()); p.setCategoryId(jdbc.queryForObject("SELECT min(id) FROM categories",Long.class));
        p.setTitle("订单测试商品"+UUID.randomUUID()); p.setDescription("真实交易测试"); p.setPrice(new BigDecimal("78.90"));
        p.setConditionLevel(4); p.setStatus("ON_SALE"); products.insert(p); return p;
    }
}
