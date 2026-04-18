package com.hrm.hrmsystem.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseSchemaFix {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void fixLeaveTypeColumn() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check current column size
            ResultSet rs = stmt.executeQuery(
                "SELECT CHARACTER_MAXIMUM_LENGTH " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_NAME = 'leaves' AND COLUMN_NAME = 'leave_type'"
            );

            if (rs.next()) {
                int currentLength = rs.getInt(1);
                System.out.println("Current leave_type column size: " + currentLength);

                if (currentLength < 20) {
                    // Alter column to increase size
                    stmt.executeUpdate("ALTER TABLE leaves MODIFY leave_type VARCHAR(20) NOT NULL");
                    System.out.println("✅ Fixed: leave_type column resized to VARCHAR(20)");
                } else {
                    System.out.println("✅ leave_type column already correct size");
                }
            }
        } catch (Exception e) {
            System.err.println("Error fixing leave_type column: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
