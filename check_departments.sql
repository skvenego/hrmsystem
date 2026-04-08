-- Check what departments exist in database
SELECT '=== Departments in Database ===' as info;
SELECT id, name FROM departments ORDER BY id;

-- Check if department_id 5 exists
SELECT '=== Check Department ID 5 ===' as info;
SELECT * FROM departments WHERE id = 5;

-- Show all department IDs
SELECT '=== All Department IDs ===' as info;
SELECT id, name FROM departments WHERE name LIKE '%Sales%' OR name LIKE '%IT%' OR name LIKE '%HR%' OR name LIKE '%Finance%';
