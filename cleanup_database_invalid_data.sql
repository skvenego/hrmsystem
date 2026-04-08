-- ============================================
-- DATABASE CLEANUP SCRIPT
-- HRM System - Remove Invalid/Unknown Data
-- ============================================
-- This script removes:
-- 1. Payslips with NULL employee or ZERO salary
-- 2. Duplicate payroll records
-- 3. Orphaned attendance records
-- 4. Invalid leave records
-- ============================================

USE hrm_db;

-- ============================================
-- STEP 1: Backup Important Tables (Optional)
-- ============================================
-- Uncomment these if you want to create backups first

-- CREATE TABLE payslips_backup AS SELECT * FROM payslips;
-- CREATE TABLE payroll_backup AS SELECT * FROM payroll;
-- CREATE TABLE attendance_backup AS SELECT * FROM attendance;

-- ============================================
-- STEP 2: Delete Invalid Payslips
-- ============================================

-- Delete payslips with NULL employee_id
DELETE FROM payslips 
WHERE employee_id IS NULL;

-- Delete payslips with ZERO basic salary
DELETE FROM payslips 
WHERE basic_salary = 0 OR basic_salary IS NULL;

-- Delete payslips with ZERO net salary
DELETE FROM payslips 
WHERE net_salary = 0 OR net_salary IS NULL;

-- Delete duplicate payslips (keep only the latest by id for same employee+month)
DELETE p1 FROM payslips p1
INNER JOIN payslips p2 
WHERE p1.id < p2.id 
  AND p1.employee_id = p2.employee_id 
  AND p1.month_year = p2.month_year;

-- ============================================
-- STEP 3: Clean Up Payroll Records
-- ============================================

-- Delete payroll records with NULL employee
DELETE FROM payroll 
WHERE employee_id IS NULL;

-- Delete payroll records with ZERO net salary
DELETE FROM payroll 
WHERE net_salary = 0 OR net_salary IS NULL;

-- Delete duplicate payroll records (keep latest by id for same employee+month+year)
DELETE p1 FROM payroll p1
INNER JOIN payroll p2 
WHERE p1.id < p2.id 
  AND p1.employee_id = p2.employee_id 
  AND p1.month = p2.month 
  AND p1.year = p2.year;

-- ============================================
-- STEP 4: Clean Up Attendance Records
-- ============================================

-- Delete attendance records with NULL employee
DELETE FROM attendance 
WHERE employee_id IS NULL;

-- Delete attendance records with NULL date
DELETE FROM attendance 
WHERE date IS NULL;

-- Delete future attendance records (beyond today)
DELETE FROM attendance 
WHERE date > CURDATE();

-- ============================================
-- STEP 5: Clean Up Leave Records
-- ============================================

-- Delete leave records with NULL employee
DELETE FROM leave_table 
WHERE employee_id IS NULL;

-- Delete leave records with NULL start_date
DELETE FROM leave_table 
WHERE start_date IS NULL;

-- Delete cancelled leave records older than 6 months
DELETE FROM leave_table 
WHERE status = 'CANCELLED' 
  AND end_date < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);

-- ============================================
-- STEP 6: Clean Up Users Table (if needed)
-- ============================================

-- WARNING: Be very careful with users table!
-- Only delete users that have no employee relationship

-- Delete users with role 'EMPLOYEE' that have no matching employee record
-- (Only if you're sure they shouldn't exist)
/*
DELETE u FROM users u
LEFT JOIN employees e ON u.id = e.user_id
WHERE u.role = 'EMPLOYEE' 
  AND e.id IS NULL
  AND u.username NOT IN ('admin'); -- Keep admin user
*/

-- ============================================
-- STEP 7: Show Cleanup Results
-- ============================================

-- Count remaining records in each table
SELECT 
    'payslips' as table_name, 
    COUNT(*) as record_count 
FROM payslips
UNION ALL
SELECT 'payroll', COUNT(*) FROM payroll
UNION ALL
SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL
SELECT 'leave_table', COUNT(*) FROM leave_table
UNION ALL
SELECT 'employees', COUNT(*) FROM employees
UNION ALL
SELECT 'users', COUNT(*) FROM users
ORDER BY table_name;

-- ============================================
-- STEP 8: Verify Data Quality
-- ============================================

-- Check for any remaining invalid payslips
SELECT 
    'Invalid Payslips Found' as issue,
    COUNT(*) as count
FROM payslips
WHERE employee_id IS NULL 
   OR basic_salary = 0 
   OR net_salary = 0;

-- Check for any remaining invalid payroll
SELECT 
    'Invalid Payroll Found' as issue,
    COUNT(*) as count
FROM payroll
WHERE employee_id IS NULL 
   OR net_salary = 0;

-- Check for orphaned attendance
SELECT 
    'Orphaned Attendance' as issue,
    COUNT(*) as count
FROM attendance
WHERE employee_id IS NULL;

-- ============================================
-- STEP 9: Optimize Tables (Optional)
-- ============================================

-- Reclaim space after deletions
OPTIMIZE TABLE payslips;
OPTIMIZE TABLE payroll;
OPTIMIZE TABLE attendance;
OPTIMIZE TABLE leave_table;

-- ============================================
-- CLEANUP COMPLETE!
-- ============================================
-- Your database is now cleaned up!
-- All invalid, duplicate, and orphaned records have been removed.
-- ============================================
