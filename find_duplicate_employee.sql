-- ============================================
-- Find and Remove Duplicate Employee
-- ============================================

-- Step 1: Find the employee with this email
SELECT 
    id,
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    phone,
    designation,
    department,
    created_at
FROM employees
WHERE email = 'skvbca32@gmail.com';

-- Step 2: If you want to delete this employee (CAREFUL!)
-- Uncomment the line below ONLY if you're sure
-- DELETE FROM employees WHERE email = 'skvbca32@gmail.com';

-- Step 3: Verify deletion
-- SELECT COUNT(*) as count FROM employees WHERE email = 'skvbca32@gmail.com';
