-- ============================================
-- CHECK AND FIX ADDRESS COLUMN IN EMPLOYEES TABLE
-- ============================================

-- Step 1: Check current table structure
DESCRIBE employees;

-- Step 2: Check if address column exists
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'employees' 
  AND COLUMN_NAME = 'address';

-- Step 3: Add address column if it doesn't exist
ALTER TABLE employees 
ADD COLUMN address VARCHAR(500) DEFAULT NULL AFTER tax;

-- Step 4: Verify the column was added
DESCRIBE employees;

-- Step 5: Test update on employee ID 15 (the one we're testing)
UPDATE employees 
SET address = 'Test address update'
WHERE id = 15;

-- Step 6: Verify the update worked
SELECT 
    id, 
    firstName, 
    lastName, 
    gender, 
    pf, 
    tax, 
    address
FROM employees 
WHERE id = 15;

-- ============================================
-- EXPECTED RESULT:
-- The address column should exist and be updatable
-- ============================================
