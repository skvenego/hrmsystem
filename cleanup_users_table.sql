-- Clean up users table - Remove unnecessary columns
-- Keep only: id, username, email, password, phone, address, role, is_active, created_at, last_login

USE hrmsystem;

-- Step 1: Check current structure
SELECT 'Current users table structure:' AS Status;
DESCRIBE users;

-- Step 2: Remove unnecessary columns (first_name, last_name, department, position)
-- Note: These are already NULL in your database, so safe to drop

ALTER TABLE users DROP COLUMN IF EXISTS first_name;
ALTER TABLE users DROP COLUMN IF EXISTS last_name;
ALTER TABLE users DROP COLUMN IF EXISTS department;
ALTER TABLE users DROP COLUMN IF EXISTS position;

-- Step 3: Verify final structure
SELECT 'Final users table structure (cleaned):' AS Status;
DESCRIBE users;

-- Step 4: Show all users with remaining columns
SELECT 
    id,
    username,
    email,
    phone,
    address,
    role,
    is_active,
    created_at
FROM users
ORDER BY id;

SELECT 'Cleanup complete! Users table now has only essential fields.' AS Status;
