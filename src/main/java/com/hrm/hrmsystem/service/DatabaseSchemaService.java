package com.hrm.hrmsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

@Service
public class DatabaseSchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void ensureAddressColumnExists() {
        System.out.println("=== DATABASE SCHEMA SERVICE STARTED ===");
        System.out.println("Checking if address column exists in employees table...");
        
        try {
            // Check if address column exists
            String checkColumnSql = """
                SELECT COUNT(*) 
                FROM INFORMATION_SCHEMA.COLUMNS 
                WHERE TABLE_SCHEMA = DATABASE() 
                AND TABLE_NAME = 'employees' 
                AND COLUMN_NAME = 'address'
                """;
            
            Long count = jdbcTemplate.queryForObject(checkColumnSql, Long.class);
            System.out.println("Address column count: " + count);
            
            if (count == null || count == 0) {
                System.out.println("Address column not found in employees table. Adding it...");
                
                // Add address column
                String addColumnSql = """
                    ALTER TABLE employees 
                    ADD COLUMN address VARCHAR(500) DEFAULT NULL AFTER tax
                    """;
                
                jdbcTemplate.execute(addColumnSql);
                System.out.println("Address column added successfully to employees table.");
                
                // Verify the column was added
                Long newCount = jdbcTemplate.queryForObject(checkColumnSql, Long.class);
                System.out.println("Address column count after adding: " + newCount);
                
            } else {
                System.out.println("Address column already exists in employees table.");
            }
            
            // Show current table structure
            System.out.println("Current employees table structure:");
            jdbcTemplate.query("DESCRIBE employees", (rs) -> {
                System.out.println(rs.getString("Field") + " | " + rs.getString("Type") + " | " + rs.getString("Null"));
            });
            
        } catch (Exception e) {
            System.err.println("Error ensuring address column exists: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== DATABASE SCHEMA SERVICE COMPLETED ===");
    }
}
