-- Quick Fix for Profile/Settings showing "-" for Phone and Address
-- Run this SQL to update all users with default data

USE hrmsystem;

-- First, check what columns exist
SELECT 'Checking current users table structure...' AS Status;
DESCRIBE users;

-- Add phone column if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

-- Add address column if missing  
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT;

-- Update all users that have NULL values
UPDATE users 
SET 
    phone = COALESCE(phone, '+0000000000'),
    address = COALESCE(address, 'Not provided')
WHERE phone IS NULL OR address IS NULL;

-- Show results
SELECT 
    id,
    username,
    email,
    phone,
    address
FROM users
ORDER BY id;

SELECT 'Fix complete! Refresh your Profile page now.' AS Status;
