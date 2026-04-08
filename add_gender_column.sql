-- Add gender column to employees table
ALTER TABLE employees 
ADD COLUMN gender VARCHAR(10) DEFAULT NULL AFTER phone;

-- Verify the table structure
DESCRIBE employees;

-- Show sample data
SELECT id, first_name, last_name, email, phone, gender FROM employees LIMIT 3;
