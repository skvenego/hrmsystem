-- Verify the updated table structure
DESCRIBE employees;

-- Show sample data to confirm everything works
SELECT id, first_name, last_name, email, phone, department_id, designation, joining_date, 
       salary, basic_salary, da, hra, other_allowance, pf, tax, status, address
FROM employees 
LIMIT 3;
