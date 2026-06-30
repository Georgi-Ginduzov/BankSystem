package com.banksystem.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class LoanSettlementAccountMigration {

    @Bean
    public ApplicationRunner migrateLoanSettlementAccountColumn(JdbcTemplate jdbcTemplate) {
        return args -> {
            Integer columnCount = jdbcTemplate.query(
                    """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'loan'
                      AND COLUMN_NAME = 'settlement_account_id'
                    """,
                    rs -> rs.next() ? rs.getInt(1) : 0
            );

            if (columnCount == null || columnCount == 0) {
                jdbcTemplate.execute("ALTER TABLE loan ADD COLUMN settlement_account_id INT NULL");
            }
        };
    }
}
