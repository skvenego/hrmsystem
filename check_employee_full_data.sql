-- ============================================
-- COMPLETE EMPLOYEE DATA DELETION CHECK
-- ============================================

-- This script shows ALL data related to "Rahul" or any employee across all tables

-- Step 1: Find the employee
SELECT 
    'EMPLOYEE FOUND' as status,
    id,
    first_name,
    last_name,
    email,
    status,
    created_at
FROM employees
WHERE first_name LIKE '%Rahul%' OR last_name LIKE '%Rahul%' OR email LIKE '%Rahul%';

-- Step 2: Check if employee has payroll records
SELECT 
    'PAYROLL RECORDS' as table_name,
    p.id,
    e.first_name,
    e.last_name,
    p.month,
    p.year,
    p.net_salary,
    p.status
FROM payroll p
LEFT JOIN employees e ON p.employee_id = e.id
WHERE e.first_name LIKE '%Rahul%' OR e.last_name LIKE '%Rahul%' OR e.email LIKE '%Rahul%'
   OR p.employee_id IS NULL;

-- Step 3: Check attendance records
SELECT 
    'ATTENDANCE RECORDS' as table_name,
    a.id,
    e.first_name,
    e.last_name,
    a.date,
    a.status
FROM attendance a
LEFT JOIN employees e ON a.employee_id = e.id
WHERE e.first_name LIKE '%Rahul%' OR e.last_name LIKE '%Rahul%' OR e.email LIKE '%Rahul%'
   OR a.employee_id IS NULL;

-- Step 4: Check leave records
SELECT 
    'LEAVE RECORDS' as table_name,
    l.id,
    e.first_name,
    e.last_name,
    l.leave_type,
    l.start_date,
    l.end_date,
    l.status
FROM leaves l
LEFT JOIN employees e ON l.employee_id = e.id
WHERE e.first_name LIKE '%Rahul%' OR e.last_name LIKE '%Rahul%' OR e.email LIKE '%Rahul%'
   OR l.employee_id IS NULL;

-- Step 5: Check payslip records
SELECT 
    'PAYSLIP RECORDS' as table_name,
    ps.id,
    e.first_name,
    e.last_name,
    ps.month_year,
    ps.net_salary
FROM payslips ps
LEFT JOIN employees e ON ps.employee_id = e.id
WHERE e.first_name LIKE '%Rahul%' OR e.last_name LIKE '%Rahul%' OR e.email LIKE '%Rahul%'
   OR ps.employee_id IS NULL;

-- ============================================
-- CLEANUP OPTIONS
-- ============================================

-- OPTION 1: Delete specific employee by ID (CASCades to all related tables)
-- Replace 123 with actual employee ID
-- DELETE FROM employees WHERE id = 123;

-- OPTION 2: Delete orphaned records (employee_id IS NULL)
DELETE FROM payroll WHERE employee_id IS NULL;
DELETE FROM attendance WHERE employee_id IS NULL;
DELETE FROM leaves WHERE employee_id IS NULL;
DELETE FROM payslips WHERE employee_id IS NULL;

-- OPTION 3: Delete INACTIVE employees (soft delete approach)
-- First, mark as inactive instead of deleting
-- UPDATE employees SET status = 'INACTIVE' WHERE id = 123;

-- ============================================
-- VERIFY AFTER CLEANUP
-- ============================================

-- Show remaining employees
SELECT 
    'CURRENT EMPLOYEES' as status,
    id,
    first_name,
    last_name,
    email,
    status
FROM employees
ORDER BY id;

-- Count records in each table
SELECT 'payroll' as table_name, COUNT(*) as record_count FROM payroll
UNION ALL
SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL
SELECT 'leaves', COUNT(*) FROM leaves
UNION ALL
SELECT 'payslips', COUNT(*) FROM payslips
UNION ALL
SELECT 'employees', COUNT(*) FROM employees;
