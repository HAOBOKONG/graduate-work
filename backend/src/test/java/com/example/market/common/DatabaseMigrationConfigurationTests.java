package com.example.market.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseMigrationConfigurationTests {
    @Test void maintenanceConnectionPreservesOptions() {
        assertEquals("jdbc:postgresql://localhost:5432/postgres?sslmode=require",
                DatabaseMigrationConfiguration.maintenanceUrl("jdbc:postgresql://localhost:5432/secondhand_market?sslmode=require"));
    }
    @Test void refusesMigrationIntoOtherDatabase() {
        assertThrows(IllegalArgumentException.class, () -> DatabaseMigrationConfiguration.maintenanceUrl("jdbc:postgresql://localhost:5432/postgres"));
        assertThrows(IllegalArgumentException.class, () -> DatabaseMigrationConfiguration.maintenanceUrl("jdbc:postgresql://user:secret@localhost/secondhand_market"));
    }
}
