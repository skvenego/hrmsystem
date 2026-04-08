-- Insert test employee data to verify complete data flow
INSERT INTO employees (
    first_name, last_name, email, phone, gender, department_id, 
    designation, joining_date, salary, pf, tax, status, address
) VALUES (
    'TestUser1', 'TestName1', 'test.user1.2026@gmail.com', '9876543210', 'Male', 1,
    'Software Engineer', '2026-04-03', 50000.00, 3600.00, 5000.00, 'ACTIVE', '123 Test Street, City'
);

INSERT INTO employees (
    first_name, last_name, email, phone, gender, department_id, 
    designation, joining_date, salary, pf, tax, status, address
) VALUES (
    'TestUser2', 'TestName2', 'test.user2.2026@gmail.com', '9876543211', 'Female', 2,
    'HR Manager', '2026-04-03', 60000.00, 4320.00, 6000.00, 'ACTIVE', '456 HR Avenue, City'
);

INSERT INTO employees (
    first_name, last_name, email, phone, gender, department_id, 
    designation, joining_date, salary, pf, tax, status, address
) VALUES (
    'TestUser3', 'TestName3', 'test.user3.2026@gmail.com', '9876543212', 'Other', 3,
    'Finance Analyst', '2026-04-03', 55000.00, 3960.00, 5500.00, 'INACTIVE', '789 Finance Road, City'
);

-- Verify data was inserted correctly
SELECT '=== Test Data Inserted Successfully ===' as info;
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
WHERE email LIKE '%test.user%.2026@gmail.com%'
ORDER BY created_at DESC;

SELECT '=== Total Employees After Test Data ===' as info;
SELECT COUNT(*) as total_employees FROM employees;
