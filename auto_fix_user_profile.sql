-- Auto-Fix: Ensure users table has phone/address columns and update existing users
-- This script runs safely - if columns exist, it skips them

USE hrmsystem;

-- Step 1: Add columns if they don't exist (MySQL 8.0+ syntax)
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(50) AFTER email;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(50) AFTER first_name;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20) AFTER last_name;
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT AFTER phone;
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100) AFTER address;
ALTER TABLE users ADD COLUMN IF NOT EXISTS position VARCHAR(100) AFTER department;

-- Step 2: Update any users with NULL values to have default data
UPDATE users 
SET 
    first_name = COALESCE(first_name, 'User'),
    last_name = COALESCE(last_name, SUBSTRING_INDEX(username, '@', 1)),
    phone = COALESCE(phone, '+0000000000'),
    address = COALESCE(address, 'Not provided'),
    department = COALESCE(department, 'General'),
    position = COALESCE(position, 'Employee')
WHERE first_name IS NULL OR last_name IS NULL OR phone IS NULL OR address IS NULL;

-- Step 3: Show all users with their data
SELECT 
    id,
    username,
    email,
    first_name,
    last_name,
    phone,
    address,
    department,
    position,
    role
FROM users
ORDER BY id;
