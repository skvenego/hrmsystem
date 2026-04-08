-- Check all employees and their IDs
SELECT 
    id,
    CONCAT(first_name, ' ', last_name) as employee_name,
    email,
    designation,
    department,
    status,
    joining_date,
    created_at
FROM employees
ORDER BY id DESC;

-- Count total employees
SELECT COUNT(*) as total_employees FROM employees;

-- Check if Sachin exists
SELECT 'SACHIN FOUND' as status, id, email, created_at 
FROM employees 
WHERE first_name LIKE '%sachin%' OR last_name LIKE '%sachin%' OR email LIKE '%sachin%';
