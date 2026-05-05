-- Diagnose and Fix Employee ID mismatch
USE hrm_db;

-- Step 1: Find Anubhav Sharma in employees table
SELECT id, first_name, last_name, email, joining_date, status 
FROM employees 
WHERE email LIKE '%sharma%' OR first_name LIKE '%Anubhav%' OR last_name LIKE '%sharma%';

-- Step 2: Check users table for skvbca32@gmail.com
SELECT u.id, u.username, u.email, u.employee_id, e.first_name, e.last_name
FROM users u
LEFT JOIN employees e ON u.employee_id = e.id
WHERE u.email = 'skvbca32@gmail.com' OR u.username = 'skvbca32@gmail.com';

-- Step 3: Check which employee has the leaves (Employee 16)
SELECT e.id, e.first_name, e.last_name, e.email
FROM employees e
WHERE e.id = 16;

-- Step 4: If Employee 16 is the correct employee (Anubhav Sharma), 
-- then we need to update the user's employee_id to 16
-- OR if the employee doesn't exist, we need to create it

-- Check if Employee 51 exists
SELECT * FROM employees WHERE id = 51;

-- Step 5: Fix - Update the user to point to the correct employee (16)
-- UPDATE users SET employee_id = 16 WHERE email = 'skvbca32@gmail.com';

-- Or create an employee record for user 51 if it doesn't exist
-- INSERT INTO employees (id, first_name, last_name, email, joining_date, department, designation, status, probation_period_months)
-- VALUES (51, 'Anubhav', 'Sharma', 'skvbca32@gmail.com', '2025-10-05', 'Operations', 'provide info..', 'ACTIVE', 3);

-- Step 6: After fixing the employee record, move the leaves
-- UPDATE leaves SET employee_id = (correct_employee_id) WHERE employee_id = 16;
