-- Check if vivekkumar123@gmail.com exists in database
SELECT '=== Checking Email Existence ===' as info;

-- Search for the specific email
SELECT id, first_name, last_name, email, status, created_at
FROM employees 
WHERE email = 'vivekkumar123@gmail.com';

-- Show all existing emails to see what's taken
SELECT '=== All Existing Emails ===' as info;
SELECT id, email, first_name, last_name, status, created_at
FROM employees 
ORDER BY created_at DESC;

-- Count total employees
SELECT '=== Total Employee Count ===' as info;
SELECT COUNT(*) as total_employees FROM employees;
