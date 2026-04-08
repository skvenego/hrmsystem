-- Verify the new employee data was stored correctly
SELECT '=== Latest Employee Added Successfully ===' as info;
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
WHERE id = 15;

SELECT '=== Verify Department Mapping ===' as info;
SELECT e.id, e.first_name, e.last_name, e.department_id, d.name as department_name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
WHERE e.id = 15;
