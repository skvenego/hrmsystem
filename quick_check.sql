-- Quick check for recent data
SELECT 
    id,
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    salary,
    pf,
    tax,
    status,
    created_at
FROM employees 
ORDER BY created_at DESC 
LIMIT 5;
