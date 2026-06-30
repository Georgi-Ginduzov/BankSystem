package com.banksystem.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class EmployeeIdentifierColumnMigration {

    @Bean
    public ApplicationRunner migrateEmployeeIdentifierColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            Integer maxLength = jdbcTemplate.query(
                    """
                    SELECT CHARACTER_MAXIMUM_LENGTH
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'employee'
                      AND COLUMN_NAME = 'ucn'
                    """,
                    rs -> rs.next() ? rs.getInt("CHARACTER_MAXIMUM_LENGTH") : null
            );

            if (maxLength != null && maxLength < 13) {
                jdbcTemplate.execute("ALTER TABLE employee MODIFY COLUMN ucn VARCHAR(13) NOT NULL");
            }
        };
    }
}
