-- Find Anubhav Sharma's correct employee ID
USE hrm_db;

-- Find employee by email pattern
SELECT id, first_name, last_name, email, joining_date 
FROM employees 
WHERE email LIKE '%sharma%' OR first_name LIKE '%Anubhav%';

-- Check all employees
SELECT id, first_name, last_name, email FROM employees ORDER BY id;

-- Check which employee has the leaves
SELECT l.id, l.employee_id, e.first_name, e.last_name, e.email, 
       l.start_date, l.end_date, l.status, l.paid_days, l.unpaid_days
FROM leaves l
LEFT JOIN employees e ON l.employee_id = e.id;
