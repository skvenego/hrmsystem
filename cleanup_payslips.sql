-- ============================================
-- CLEANUP PAYSLIPS TABLE
-- Remove unknown/incorrect data
-- ============================================

USE hrmsystem;

-- 1. First, let's see what's in the payslips table
SELECT 'Current Payslips Data:' AS Info;
SELECT id, employee_id, month_year, basic_salary, gross_salary, net_salary, created_date
FROM payslip
ORDER BY created_date DESC;

-- 2. Delete all existing payslips (to clear unknown data)
DELETE FROM payslip;

-- 3. Verify deletion
SELECT 'After Cleanup - Payslips count:' AS Info, COUNT(*) as Count FROM payslip;

-- 4. Show employees that should have payslips
SELECT 'Employees available for payslips:' AS Info;
SELECT id, first_name, last_name, email, salary, department_id
FROM employee
WHERE status = 'ACTIVE' AND salary IS NOT NULL AND salary > 0
ORDER BY id;

-- ============================================
-- REGENERATE CORRECT PAYSLIP DATA
-- ============================================

-- The payslips will be automatically regenerated when you:
-- 1. Go to Payroll page
-- 2. Click "Generate Payroll"
-- 3. Select current month and year
-- 4. Click "Generate"

-- This ensures correct calculation based on:
-- - Employee salary
-- - Attendance records
-- - Proper deductions (PF, Tax, Insurance)
-- - Allowances (HRA, DA, TA)
