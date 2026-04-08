-- Delete specific employee by email
DELETE FROM employees WHERE email = 'radharani@gmail.com';

-- Or delete multiple employees by name pattern
DELETE FROM employees WHERE first_name = 'radha' AND last_name = 'rani';

-- Show remaining employees
SELECT '=== Remaining Employees ===' as info;
SELECT id, CONCAT(first_name, ' ', last_name) as full_name, email, status FROM employees ORDER BY created_at DESC;

-- Count total employees
SELECT '=== Total Count ===' as info;
SELECT COUNT(*) as total_employees FROM employees;
