package com.example.market;

import com.example.market.mapper.UserMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** 仅使用真实 PostgreSQL；读取结构和 users，不写入虚构业务数据。 */
@SpringBootTest(classes = com.example.secondhandmarket.SecondhandMarketApplication.class)
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class DatabaseIntegrationTests {
    @Autowired UserMapper users;
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @Test void realMigrationAndUserMapperWork() {
        assertEquals("secondhand_market", jdbc.queryForObject("SELECT current_database()", String.class));
        assertNotNull(flyway.info().current());
        assertEquals("3", flyway.info().current().getVersion().getVersion());
        for (String table : new String[]{"users","categories","products","product_images","favorites","orders",
                "order_events","conversations","messages","reports","payment_records","audit_logs"}) {
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name=?", Integer.class, table));
        }
        assertEquals(jdbc.queryForObject("SELECT count(*) FROM users", Long.class), users.selectCount(null));
        assertNotNull(users.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.example.market.entity.User>().last("LIMIT 1")));
    }
}
