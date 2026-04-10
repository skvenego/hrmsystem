-- ============================================
-- HRM SYSTEM - DATABASE FIX SCRIPT
-- Password for all test accounts: Sachin@123
-- Run this in MySQL Workbench or MySQL CLI
-- ============================================

USE hrm_db;

-- ============================================
-- 1. CHECK CURRENT USERS TABLE STRUCTURE
-- ============================================
SELECT '--- Current Users ---' as info;
SELECT username, email, role FROM users ORDER BY role, username;

-- ============================================
-- 2. DELETE TEST USERS IF THEY EXIST (TO AVOID CONFLICTS)
-- ============================================
SELECT '--- Deleting old test users ---' as info;
DELETE FROM users WHERE username IN ('admin', 'hradmin', 'employee1');

-- ============================================
-- 3. CREATE NEW TEST USERS WITH PASSWORD: Sachin@123
-- ============================================
SELECT '--- Creating new test users ---' as info;

-- Admin user (Full Access)
INSERT INTO users (username, email, password, role) 
VALUES (
    'admin',
    'admin@company.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO',
    'ROLE_ADMIN'
);

-- HR Admin user (Management Access)
INSERT INTO users (username, email, password, role) 
VALUES (
    'hradmin',
    'hr@company.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO',
    'ROLE_HR'
);

-- Employee user (Limited Access)
-- First check if employee_id 2 exists
SET @emp_id = (SELECT id FROM employees WHERE id = 2 LIMIT 1);

INSERT INTO users (username, email, password, role) 
VALUES (
    'employee1',
    'employee@company.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO',
    'ROLE_EMPLOYEE'
);

-- ============================================
-- 4. UPDATE EXISTING USERS TO USE SAME PASSWORD
-- ============================================
SELECT '--- Updating existing users password ---' as info;

UPDATE users 
SET password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO'
WHERE username IN ('hr', 'accountant', 'director', 'Enego', 'Sagar', 'sachin', 'SACHIN KUMAR VERMA', 
                   'sharmaa', 'rajpoot', 'ramesh', 'arjun', 'tiwari', 'Verma', 'pradeep', 'Anubhav');

-- ============================================
-- 5. VERIFY ALL USERS
-- ============================================
SELECT '--- All Users After Fix ---' as info;
SELECT username, email, role FROM users ORDER BY role, username;

-- ============================================
-- 6. CHECK EMPLOYEES TABLE
-- ============================================
SELECT '--- Employees Table ---' as info;
SELECT id, first_name, last_name, email, department_id, status FROM employees;

-- ============================================
-- 7. VERIFY DEPARTMENTS
-- ============================================
SELECT '--- Departments ---' as info;
SELECT id, name FROM departments;

-- ============================================
-- LOGIN CREDENTIALS SUMMARY
-- ============================================
SELECT '========================================' as '=======================================';
SELECT 'ALL USERS CAN LOGIN WITH:' as 'LOGIN INFO';
SELECT 'Username: (any username from list)' as 'USERNAME';
SELECT 'Password: Sachin@123' as 'PASSWORD';
SELECT '========================================' as '=======================================';

SELECT username as 'Login Username', role as 'Role' FROM users ORDER BY FIELD(role, 'ROLE_ADMIN', 'ROLE_HR', 'ROLE_ACCOUNTANT', 'ROLE_DIRECTOR', 'ROLE_EMPLOYEE'), username;
