-- ============================================
-- HRMS Database Cleanup Script
-- Fixes duplicate payroll records and dummy data
-- Run this ONCE to clean your database
-- ============================================

USE hrmsystem;

-- ============================================
-- STEP 1: View Current Data
-- ============================================

SELECT '=== CURRENT EMPLOYEES ===' as '';
SELECT id, first_name, last_name, email, designation FROM employee ORDER BY id;

SELECT '=== CURRENT PAYROLL RECORDS (Check for Duplicates) ===' as '';
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year, p.net_salary 
FROM payroll p 
JOIN employee e ON p.employee_id = e.id 
ORDER BY p.employee_id, p.year, p.month;

SELECT '=== DUPLICATE CHECK - Payroll ===' as '';
SELECT employee_id, month, year, COUNT(*) as duplicate_count
FROM payroll
GROUP BY employee_id, month, year
HAVING duplicate_count > 1;

SELECT '=== DUPLICATE CHECK - Payslip ===' as '';
SELECT employee_id, month_year, COUNT(*) as duplicate_count
FROM payslip
GROUP BY employee_id, month_year
HAVING duplicate_count > 1;

-- ============================================
-- STEP 2: Delete Duplicate Payroll Records
-- ============================================

SELECT '=== DELETING DUPLICATE PAYROLL RECORDS ===' as '';

-- Method: Keep the record with lowest ID, delete rest
DELETE p1 FROM payroll p1
INNER JOIN payroll p2 
WHERE p1.id > p2.id 
  AND p1.employee_id = p2.employee_id 
  AND p1.month = p2.month 
  AND p1.year = p2.year;

-- Verify deletion
SELECT '=== DUPLICATES AFTER CLEANUP (should be empty) ===' as '';
SELECT employee_id, month, year, COUNT(*) as count
FROM payroll
GROUP BY employee_id, month, year
HAVING count > 1;

-- ============================================
-- STEP 3: Delete Duplicate Payslip Records
-- ============================================

SELECT '=== DELETING DUPLICATE PAYSLIP RECORDS ===' as '';

-- Method: Keep the record with lowest ID, delete rest
DELETE ps1 FROM payslip ps1
INNER JOIN payslip ps2 
WHERE ps1.id > ps2.id 
  AND ps1.employee_id = ps2.employee_id 
  AND ps1.month_year = ps2.month_year;

-- Verify deletion
SELECT '=== PAYSLIP DUPLICATES AFTER CLEANUP (should be empty) ===' as '';
SELECT employee_id, month_year, COUNT(*) as count
FROM payslip
GROUP BY employee_id, month_year
HAVING count > 1;

-- ============================================
-- STEP 4: Add Unique Constraints (Prevent Future Duplicates)
-- ============================================

SELECT '=== ADDING UNIQUE CONSTRAINTS ===' as '';

-- Check if payroll constraint exists
SET @payroll_constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'hrmsystem' 
      AND TABLE_NAME = 'payroll' 
      AND CONSTRAINT_NAME = 'unique_employee_month_year'
);

SET @sql = IF(@payroll_constraint_exists = 0,
    'ALTER TABLE payroll ADD UNIQUE KEY unique_employee_month_year (employee_id, month, year)',
    'SELECT ''Payroll unique constraint already exists'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if payslip constraint exists
SET @payslip_constraint_exists = (
    SELECT COUNT(*) 
    FROM information_schema.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'hrmsystem' 
      AND TABLE_NAME = 'payslip' 
      AND CONSTRAINT_NAME = 'unique_employee_month_year'
);

SET @sql = IF(@payslip_constraint_exists = 0,
    'ALTER TABLE payslip ADD UNIQUE KEY unique_employee_month_year (employee_id, month_year)',
    'SELECT ''Payslip unique constraint already exists'' as message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- STEP 5: Verify Final State
-- ============================================

SELECT '=== FINAL PAYROLL RECORDS (No Duplicates) ===' as '';
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year, p.net_salary 
FROM payroll p 
JOIN employee e ON p.employee_id = e.id 
ORDER BY p.employee_id, p.year, p.month;

SELECT '=== FINAL PAYSLIP RECORDS (No Duplicates) ===' as '';
SELECT ps.id, ps.employee_id, e.first_name, e.last_name, ps.month_year, ps.net_salary 
FROM payslip ps 
JOIN employee e ON ps.employee_id = e.id 
ORDER BY ps.employee_id, ps.month_year;

SELECT '=== CLEANUP COMPLETE ===' as '';
SELECT 'SUCCESS! No more duplicate records.' as '';
SELECT 'Refresh your browser (Ctrl+F5) to see clean data.' as '';
