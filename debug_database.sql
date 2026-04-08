-- Check if new employee data is reaching the database correctly
SELECT '=== Recent Employee Additions ===' as info;

-- Show last 5 employees added with all details
SELECT 
    id,
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    phone,
    department_id,
    designation,
    joining_date,
    salary,
    pf,
    tax,
    status,
    created_at,
    updated_at
FROM employees 
ORDER BY created_at DESC 
LIMIT 5;

-- Check if any employees have NULL or missing data
SELECT '=== Data Quality Check ===' as info;
SELECT 
    COUNT(*) as total_employees,
    COUNT(CASE WHEN email IS NULL OR email = '' THEN 1 END) as null_emails,
    COUNT(CASE WHEN first_name IS NULL OR first_name = '' THEN 1 END) as null_first_names,
    COUNT(CASE WHEN last_name IS NULL OR last_name = '' THEN 1 END) as null_last_names,
    COUNT(CASE WHEN salary IS NULL OR salary = 0 THEN 1 END) as null_salaries
FROM employees;

-- Check for specific email that should be there
SELECT '=== Search for radharani@gmail.com ===' as info;
SELECT * FROM employees WHERE email = 'radharani@gmail.com';

-- Check database connection and table status
SELECT '=== Database Status ===' as info;
SHOW TABLE STATUS LIKE 'employees';
