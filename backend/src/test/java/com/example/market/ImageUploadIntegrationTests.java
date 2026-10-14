package com.example.market;

import com.example.market.entity.User;
import com.example.market.mapper.UserMapper;
import com.example.market.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.example.secondhandmarket.SecondhandMarketApplication.class,
    properties = "app.upload.dir=target/test-uploads")
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
@Transactional
class ImageUploadIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserMapper users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService tokens;

    @Test void uploadAndPublishWithLocalImage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        byte[] png = output.toByteArray();
        MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png", png);
        mvc.perform(multipart("/api/uploads/images").file(image)).andExpect(status().isUnauthorized());
        User seller = new User();
        seller.setUsername("upload_" + UUID.randomUUID().toString().replace("-", ""));
        seller.setNickname("图片测试用户");
        seller.setPassword(passwords.encode("UploadTest_2026!"));
        seller.setRole("USER"); seller.setStatus("ACTIVE"); users.insert(seller);
        String authorization = "Bearer " + tokens.issue(seller);

        String body = mvc.perform(multipart("/api/uploads/images").file(image)
            .header("Authorization", authorization)).andExpect(status().isCreated())
            .andExpect(jsonPath("url").isString()).andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).path("url").asText();
        Path stored = Path.of("target/test-uploads", url.substring(url.lastIndexOf('/') + 1));
        try {
            assertTrue(Files.isRegularFile(stored));
            mvc.perform(get(url)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
            long category = jdbc.queryForObject("SELECT min(id) FROM categories", Long.class);
            mvc.perform(post("/api/products").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                    "title", "本地上传图片商品", "description", "图片上传集成测试", "categoryId", category,
                    "price", "29.90", "conditionLevel", 4, "imageUrls", new String[]{url}))))
                .andExpect(status().isCreated()).andExpect(jsonPath("product.images[0]").value(url));
            mvc.perform(multipart("/api/uploads/images").file(new MockMultipartFile(
                "file", "bad.png", "image/png", "not an image".getBytes())).header("Authorization", authorization))
                .andExpect(status().isBadRequest());
            mvc.perform(multipart("/api/uploads/images").file(new MockMultipartFile(
                "file", "bad.svg", "image/svg+xml", png)).header("Authorization", authorization))
                .andExpect(status().isBadRequest());
            mvc.perform(multipart("/api/uploads/images").file(new MockMultipartFile(
                "file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                .header("Authorization", authorization)).andExpect(status().isBadRequest());
            mvc.perform(get("/api/uploads/images/not-a-generated-name.png"))
                .andExpect(status().isNotFound());
        } finally {
            Files.deleteIfExists(stored);
        }
    }
}
