-- Fix employee table structure to match form fields completely
ALTER TABLE employees 
ADD COLUMN pf DECIMAL(12,2) DEFAULT 0 AFTER other_allowance,
ADD COLUMN tax DECIMAL(12,2) DEFAULT 0 AFTER pf,
ADD COLUMN address TEXT AFTER tax;

-- Remove incorrect user_id column if it exists
ALTER TABLE employees DROP COLUMN IF EXISTS user_id;

-- Update NULL department_ids to valid department IDs (1-6)
UPDATE employees SET department_id = 1 WHERE department_id IS NULL AND designation LIKE '%Developer%';
UPDATE employees SET department_id = 1 WHERE department_id IS NULL AND designation LIKE '%web%';
UPDATE employees SET department_id = 2 WHERE department_id IS NULL AND designation LIKE '%HR%';
UPDATE employees SET department_id = 3 WHERE department_id IS NULL AND designation LIKE '%loan%';
UPDATE employees SET department_id = 4 WHERE department_id IS NULL AND designation LIKE '%support%';

-- Show updated table structure
DESCRIBE employees;

-- Show sample data
SELECT id, first_name, last_name, email, phone, department_id, designation, joining_date, 
       salary, basic_salary, da, hra, other_allowance, pf, tax, status, address
FROM employees 
LIMIT 5;
