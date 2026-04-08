DESCRIBE employees;

SELECT '=== Recent Employee Data ===' as info;
SELECT 
    id,
    first_name,
    last_name,
    email,
    phone,
    gender,
    department_id,
    designation,
    joining_date,
    salary,
    pf,
    tax,
    status,
    address,
    created_at
FROM employees 
ORDER BY created_at DESC 
LIMIT 5;

SELECT '=== Total Employees Count ===' as info;
SELECT COUNT(*) as total_employees FROM employees;
