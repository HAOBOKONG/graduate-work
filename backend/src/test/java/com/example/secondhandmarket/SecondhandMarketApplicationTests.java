package com.example.secondhandmarket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 无密码时验证 Web 工程装配；真实数据库验证见 DatabaseIntegrationTests。 */
@SpringBootTest(properties = {"spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "spring.datasource.hikari.minimum-idle=0"})
class SecondhandMarketApplicationTests {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.market.mapper.UserMapper userMapper;
    @Test void contextLoads() { org.junit.jupiter.api.Assertions.assertNotNull(userMapper); }
}
