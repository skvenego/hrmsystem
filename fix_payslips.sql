USE hrm_db;

-- ============================================
-- FIX DUMMY PAYSLIP DATA
-- This script will delete old generated payslips
-- so they can be regenerated with real salary data
-- ============================================

-- Step 1: Show current payslips with zeros
SELECT 'BEFORE FIX - Current Payslips:' as '';
SELECT id, employee_id, month_year, basic_salary, da, hra, other_allowance, 
       gross_salary, pf, total_deduction, net_salary, status 
FROM payslips 
ORDER BY id;

-- Step 2: Show employee salary data (the REAL data that should appear)
SELECT 'Employee Salary Data (Source of Truth):' as '';
SELECT id, CONCAT(first_name, ' ', last_name) as employee_name, email, 
       basic_salary, da, hra, other_allowance, salary as total_salary
FROM employees
ORDER BY id;

-- Step 3: Delete GENERATED payslips (will be regenerated)
SELECT 'Deleting GENERATED payslips...' as '';
DELETE FROM payslips WHERE status = 'GENERATED';

-- Step 4: Verify deletion
SELECT 'AFTER FIX - Remaining Payslips:' as '';
SELECT COUNT(*) as remaining_payslips FROM payslips;

-- Step 5: Show what's left (should only be APPROVED or SENT payslips)
SELECT 'Remaining payslip details:' as '';
SELECT id, employee_id, month_year, status, approved_by, approved_date
FROM payslips 
ORDER BY id;

SELECT 'DONE! Now regenerate payslips via API or frontend.' as '';
SELECT 'Use: POST /api/payslips/generate?employeeId=X&monthYear=2026-03' as '';
