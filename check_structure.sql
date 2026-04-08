-- Check current MySQL table structure
DESCRIBE employees;

-- Show sample data to understand field names
SELECT id, first_name, last_name, email, phone, department_id, designation, joining_date, salary, pf, tax, status, address FROM employees LIMIT 2;
