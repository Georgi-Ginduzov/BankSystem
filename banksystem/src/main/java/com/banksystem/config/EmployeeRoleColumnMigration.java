package com.banksystem.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EmployeeRoleColumnMigration {

    @Bean
    public ApplicationRunner migrateEmployeeRoleColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            String dataType = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'employee'
                      AND COLUMN_NAME = 'role'
                    """,
                    rs -> rs.next() ? rs.getString("DATA_TYPE") : null
            );

            if ("enum".equalsIgnoreCase(dataType)) {
                jdbcTemplate.execute("ALTER TABLE employee MODIFY COLUMN role VARCHAR(32) NOT NULL");
            }
        };
    }
}
