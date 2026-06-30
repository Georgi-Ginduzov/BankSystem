package com.banksystem.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LoanStatusColumnMigration {

    @Bean
    public ApplicationRunner migrateLoanStatusColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            String dataType = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'loan'
                      AND COLUMN_NAME = 'status'
                    """,
                    rs -> rs.next() ? rs.getString("DATA_TYPE") : null
            );

            Integer maxLength = jdbcTemplate.query(
                    """
                    SELECT CHARACTER_MAXIMUM_LENGTH
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'loan'
                      AND COLUMN_NAME = 'status'
                    """,
                    rs -> rs.next() ? rs.getInt("CHARACTER_MAXIMUM_LENGTH") : null
            );

            if ("enum".equalsIgnoreCase(dataType)
                    || ("varchar".equalsIgnoreCase(dataType) && (maxLength == null || maxLength < 32))) {
                jdbcTemplate.execute("ALTER TABLE loan MODIFY COLUMN status VARCHAR(32) NOT NULL");
            }
        };
    }
}
