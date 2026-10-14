package com.example.market.common;

import java.net.URI;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 在连接目标库迁移之前安全建库，不删除或覆盖任何已有数据库。 */
@Configuration
public class DatabaseMigrationConfiguration {
    static String maintenanceUrl(String targetUrl) {
        if (targetUrl == null || !targetUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("DB_URL must be a PostgreSQL network URL");
        }
        URI uri = URI.create(targetUrl.substring(5));
        if (!"/secondhand_market".equals(uri.getPath()) || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("DB_URL must target secondhand_market without embedded credentials");
        }
        int pathStart = targetUrl.indexOf('/', "jdbc:postgresql://".length());
        return targetUrl.substring(0, pathStart) + "/postgres"
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
    }

    @Bean
    FlywayMigrationStrategy safeDatabaseMigration(DataSourceProperties properties) {
        return flyway -> {
            String adminUrl = maintenanceUrl(properties.determineUrl());
            try (var connection = DriverManager.getConnection(adminUrl,
                    properties.determineUsername(), properties.determinePassword())) {
                connection.setAutoCommit(true); // PostgreSQL CREATE DATABASE cannot run in a transaction.
                boolean exists;
                try (var query = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                    query.setString(1, "secondhand_market");
                    try (var rows = query.executeQuery()) { exists = rows.next(); }
                }
                if (!exists) {
                    try (var statement = connection.createStatement()) {
                        statement.executeUpdate("CREATE DATABASE secondhand_market");
                    } catch (SQLException ex) {
                        if (!"42P04".equals(ex.getSQLState())) { throw ex; }
                    }
                }
            } catch (SQLException ex) {
                throw new IllegalStateException("Database preparation failed; check connection and CREATEDB permission. SQLState="
                        + ex.getSQLState()); // Do not expose JDBC URL or secrets.
            }
            flyway.migrate();
        };
    }
}
