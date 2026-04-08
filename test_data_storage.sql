-- Test if new employee data is being stored correctly
SELECT '=== Test Data Storage ===' as info;

-- Check if any employees added recently
SELECT 
    'Recent Additions' as check_type,
    COUNT(*) as count,
    MAX(created_at) as latest_addition
FROM employees 
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- Check data quality
SELECT '=== Data Quality Check ===' as info;
SELECT 
    COUNT(*) as total,
    COUNT(CASE WHEN first_name IS NULL OR first_name = '' THEN 1 END) as missing_first_name,
    COUNT(CASE WHEN last_name IS NULL OR last_name = '' THEN 1 END) as missing_last_name,
    COUNT(CASE WHEN email IS NULL OR email = '' THEN 1 END) as missing_email,
    COUNT(CASE WHEN gender IS NULL THEN 1 END) as missing_gender,
    COUNT(CASE WHEN department_id IS NULL THEN 1 END) as missing_department,
    COUNT(CASE WHEN joining_date IS NULL THEN 1 END) as missing_joining_date,
    COUNT(CASE WHEN salary IS NULL OR salary = 0 THEN 1 END) as missing_salary
FROM employees;

-- Show sample of latest employee data
SELECT '=== Latest Employee Sample ===' as info;
SELECT 
    id,
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    phone,
    gender,
    designation,
    joining_date,
    salary,
    pf,
    tax,
    status,
    created_at
FROM employees 
ORDER BY created_at DESC 
LIMIT 3;
