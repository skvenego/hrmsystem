-- Complete verification of employee data storage
SELECT '=== Current Table Structure ===' as info;
DESCRIBE employees;

SELECT '=== Test Data Insertion Verification ===' as info;
-- This shows what fields will be stored when you submit the form
SELECT 
    'firstName' as field_name,
    'first_name' as db_column,
    'TEXT' as data_type,
    'Required' as validation
UNION ALL
SELECT 
    'lastName',
    'last_name', 
    'TEXT',
    'Required'
UNION ALL
SELECT 
    'email',
    'email',
    'TEXT', 
    'Required'
UNION ALL
SELECT 
    'phone',
    'phone',
    'TEXT',
    'Optional'
UNION ALL
SELECT 
    'gender',
    'gender',
    'VARCHAR(10)',
    'Optional'
UNION ALL
SELECT 
    'departmentId',
    'department_id',
    'BIGINT',
    'Optional'
UNION ALL
SELECT 
    'departmentName',
    'department_name (via join)',
    'TEXT',
    'Optional'
UNION ALL
SELECT 
    'designation',
    'designation',
    'VARCHAR(100)',
    'Optional'
UNION ALL
SELECT 
    'joiningDate',
    'joining_date',
    'DATE',
    'Required'
UNION ALL
SELECT 
    'salary',
    'salary',
    'DECIMAL',
    'Required'
UNION ALL
SELECT 
    'pf',
    'pf',
    'DECIMAL',
    'Optional'
UNION ALL
SELECT 
    'tax',
    'tax',
    'DECIMAL',
    'Optional'
UNION ALL
SELECT 
    'status',
    'status',
    'ENUM',
    'Optional'
UNION ALL
SELECT 
    'address',
    'address',
    'TEXT',
    'Optional';

SELECT '=== Sample Recent Data ===' as info;
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
LIMIT 3;
