-- ============================================
-- FIX PROFILE/SETTINGS DATA ISSUE
-- Check and Sync Users vs Employees Tables
-- ============================================
-- Problem: Profile/Settings shows old data
-- Cause: Confusion between users.* and employees.* tables
-- Solution: Verify schema and sync data
-- ============================================

USE hrm_db;

-- ============================================
-- STEP 1: Check Current Schema
-- ============================================

-- Show columns in users table
SELECT 'USERS TABLE COLUMNS:' as info;
DESCRIBE users;

-- Show columns in employees table  
SELECT '\nEMPLOYEES TABLE COLUMNS:' as info;
DESCRIBE employees;

-- ============================================
-- STEP 2: Check Current Data
-- ============================================

-- See what's in users table (phone, address)
SELECT 
    id,
    username,
    CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')) as full_name,
    phone,
    address,
    department,
    position
FROM users
ORDER BY id;

-- See what's in employees table
SELECT 
    id,
    user_id,
    CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')) as full_name,
    salary,
    status
FROM employees
ORDER BY id;

-- ============================================
-- STEP 3: Find Mismatched Data
-- ============================================

-- Find users where employee has different name
SELECT 
    u.id as user_id,
    CONCAT(u.first_name, ' ', u.last_name) as user_name,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    u.phone as user_phone,
    u.address as user_address
FROM users u
LEFT JOIN employees e ON u.employee_id = e.id
WHERE u.id IS NOT NULL
  AND (u.first_name != e.first_name OR u.last_name != e.last_name);

-- ============================================
-- STEP 4: Fix - Copy Data FROM Employee TO Users
-- ============================================
-- This ensures users table has latest employee data

UPDATE users u
INNER JOIN employees e ON u.employee_id = e.id
SET 
    u.first_name = COALESCE(u.first_name, e.first_name),
    u.last_name = COALESCE(u.last_name, e.last_name)
WHERE u.employee_id IS NOT NULL
  AND (u.first_name IS NULL OR u.first_name = '');

-- ============================================
-- STEP 5: Clear Old/Incorrect Data (Optional)
-- ============================================
-- WARNING: Only run this if you want to reset everything

/*
-- Clear all profile data from users table (to start fresh)
UPDATE users 
SET first_name = NULL, 
    last_name = NULL, 
    phone = NULL, 
    address = NULL,
    department = NULL,
    position = NULL;
*/

-- ============================================
-- STEP 6: Verify Phone/Address Columns Exist
-- ============================================

-- Check if phone and address columns exist in users table
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hrm_db'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME IN ('phone', 'address', 'first_name', 'last_name');

-- ============================================
-- STEP 7: Add Columns If Missing (Critical!)
-- ============================================
-- Run these ONLY if columns don't exist

/*
-- Add phone column if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20) AFTER last_name;

-- Add address column if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(255) AFTER phone;

-- Add department column if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100) AFTER address;

-- Add position column if missing
ALTER TABLE users ADD COLUMN IF NOT EXISTS position VARCHAR(100) AFTER department;
*/

-- ============================================
-- STEP 8: Test Data Insertion
-- ============================================

-- Test updating a user's profile (use your user ID)
-- UPDATE users 
-- SET phone = '+1-555-TEST', 
--     address = '123 Test Street'
-- WHERE id = 1; -- Change to your actual user ID

-- Verify the update worked
-- SELECT id, username, phone, address FROM users WHERE id = 1;

-- ============================================
-- STEP 9: Show Final Status
-- ============================================

SELECT 
    u.id,
    u.username,
    CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as name_from_users,
    CONCAT(COALESCE(e.first_name, ''), ' ', COALESCE(e.last_name, '')) as name_from_employees,
    u.phone,
    u.address,
    CASE 
        WHEN u.phone IS NOT NULL THEN '✅ Has phone'
        ELSE '❌ No phone'
    END as phone_status,
    CASE 
        WHEN u.address IS NOT NULL THEN '✅ Has address'
        ELSE '❌ No address'
    END as address_status
FROM users u
LEFT JOIN employees e ON u.employee_id = e.id
ORDER BY u.id;

-- ============================================
-- CLEANUP COMPLETE!
-- ============================================
-- Your users table should now have proper columns
-- and data synced with employees table.
-- ============================================
