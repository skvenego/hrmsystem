-- Check if new employee data is being saved to database
SELECT '=== Recent Employees Added ===' as info;

-- Show all employees with their data
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
    address,
    created_at
FROM employees 
ORDER BY created_at DESC 
LIMIT 10;

-- Count total employees
SELECT '=== Total Employees ===' as info;
SELECT COUNT(*) as total_employees FROM employees;

-- Check for recent additions (last 5 minutes)
SELECT '=== Employees Added in Last 5 Minutes ===' as info;
SELECT 
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    created_at
FROM employees 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
ORDER BY created_at DESC;

-- Check if PF and Tax columns have data
SELECT '=== PF and Tax Data ===' as info;
SELECT 
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    salary,
    pf,
    tax,
    CASE 
        WHEN pf > 0 THEN 'PF Calculated'
        WHEN pf = 0 THEN 'PF Not Calculated'
        ELSE 'PF Missing'
    END as pf_status,
    CASE 
        WHEN tax > 0 THEN 'Tax Calculated'
        WHEN tax = 0 THEN 'Tax Not Calculated'
        ELSE 'Tax Missing'
    END as tax_status
FROM employees 
ORDER BY created_at DESC
LIMIT 5;
