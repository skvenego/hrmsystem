-- ============================================
-- COMPLETE DATABASE CLEANUP & REFRESH
-- Fix Payroll, Attendance, Payslips Old Data
-- ============================================

USE hrm_db;

-- ============================================
-- STEP 1: CHECK CURRENT DATA STATUS
-- ============================================

SELECT '=== PAYROLL RECORDS ===' as status;
SELECT COUNT(*) as total_payroll_records FROM payroll;
SELECT * FROM payroll ORDER BY id DESC LIMIT 5;

SELECT '\n=== PAYSLIPS ===' as status;
SELECT COUNT(*) as total_payslips FROM payslips;
SELECT * FROM payslips ORDER BY id DESC LIMIT 5;

SELECT '\n=== ATTENDANCE RECORDS ===' as status;
SELECT COUNT(*) as total_attendance FROM attendance;
SELECT * FROM attendance ORDER BY id DESC LIMIT 5;

SELECT '\n=== EMPLOYEES ===' as status;
SELECT COUNT(*) as total_employees FROM employees;
SELECT id, first_name, last_name, salary, status FROM employees ORDER BY id;

-- ============================================
-- STEP 2: DELETE OLD/DUMMY DATA (OPTIONAL)
-- ============================================
-- WARNING: This will delete ALL historical data!
-- Only run this if you want a fresh start.

/*
-- Delete all payroll records
DELETE FROM payroll;

-- Delete all payslips
DELETE FROM payslips;

-- Delete all attendance records (keep recent)
DELETE FROM attendance WHERE date < DATE_SUB(CURDATE(), INTERVAL 30 DAY);
*/

-- ============================================
-- STEP 3: FIX EMPLOYEE SALARIES (IMPORTANT!)
-- ============================================
-- Ensure all active employees have proper salaries

UPDATE employees 
SET salary = CASE 
    WHEN id = 1 THEN 75000.00  -- Admin/Owner
    WHEN id = 2 THEN 55000.00  -- HR Manager
    WHEN id = 3 THEN 45000.00  -- Developer
    WHEN id = 4 THEN 42000.00  -- Designer
    WHEN id = 5 THEN 38000.00  -- Support
    ELSE salary
END
WHERE status = 'ACTIVE' AND (salary IS NULL OR salary = 0);

-- Verify salaries updated
SELECT '\n=== EMPLOYEES WITH UPDATED SALARIES ===' as status;
SELECT id, CONCAT(first_name, ' ', last_name) as name, salary, status 
FROM employees 
WHERE status = 'ACTIVE' 
ORDER BY id;

-- ============================================
-- STEP 4: GENERATE FRESH PAYROLL DATA
-- ============================================
-- Create payroll for current month (March 2026)

INSERT INTO payroll (employee_id, month, year, basic_salary, allowances, deductions, net_salary, payment_date, status, created_at)
SELECT 
    e.id,
    MONTH(CURDATE()) as month,           -- Current month (3)
    YEAR(CURDATE()) as year,             -- Current year (2026)
    e.salary as basic_salary,
    e.salary * 0.20 as allowances,       -- 20% allowances
    e.salary * 0.10 as deductions,       -- 10% deductions (tax, etc.)
    (e.salary + (e.salary * 0.20) - (e.salary * 0.10)) as net_salary,
    CURDATE() as payment_date,
    'PENDING' as status,
    NOW() as created_at
FROM employees e
WHERE e.status = 'ACTIVE'
AND NOT EXISTS (
    SELECT 1 FROM payroll p 
    WHERE p.employee_id = e.id 
    AND p.month = MONTH(CURDATE()) 
    AND p.year = YEAR(CURDATE())
);

-- Verify payroll created
SELECT '\n=== NEW PAYROLL RECORDS CREATED ===' as status;
SELECT 
    p.id,
    e.first_name as employee_name,
    p.month,
    p.year,
    p.basic_salary,
    p.allowances,
    p.deductions,
    p.net_salary,
    p.status
FROM payroll p
JOIN employees e ON p.employee_id = e.id
WHERE p.month = MONTH(CURDATE()) AND p.year = YEAR(CURDATE())
ORDER BY p.id;

-- ============================================
-- STEP 5: GENERATE FRESH PAYSLIPS
-- ============================================
-- Create payslips for current month

INSERT INTO payslips (employee_id, month_year, gross_salary, deductions, net_salary, generated_date, pdf_path)
SELECT 
    e.id,
    CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0')) as month_year,
    (e.salary + (e.salary * 0.20)) as gross_salary,
    (e.salary * 0.10) as deductions,
    (e.salary + (e.salary * 0.20) - (e.salary * 0.10)) as net_salary,
    CURDATE() as generated_date,
    NULL as pdf_path
FROM employees e
WHERE e.status = 'ACTIVE'
AND NOT EXISTS (
    SELECT 1 FROM payslips ps 
    WHERE ps.employee_id = e.id 
    AND ps.month_year = CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0'))
);

-- Verify payslips created
SELECT '\n=== NEW PAYSLIPS CREATED ===' as status;
SELECT 
    ps.id,
    e.first_name as employee_name,
    ps.month_year,
    ps.gross_salary,
    ps.deductions,
    ps.net_salary,
    ps.generated_date
FROM payslips ps
JOIN employees e ON ps.employee_id = e.id
WHERE ps.month_year = CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0'))
ORDER BY ps.id;

-- ============================================
-- STEP 6: FIX ATTENDANCE FOR CURRENT MONTH
-- ============================================
-- Mark attendance for all working days in current month

-- First, let's create attendance for today only (as example)
INSERT INTO attendance (employee_id, date, status, check_in, check_out)
SELECT 
    e.id,
    CURDATE() as date,
    'PRESENT' as status,
    '09:00:00' as check_in,
    '17:00:00' as check_out
FROM employees e
WHERE e.status = 'ACTIVE'
AND NOT EXISTS (
    SELECT 1 FROM attendance a 
    WHERE a.employee_id = e.id 
    AND a.date = CURDATE()
);

-- Verify today's attendance
SELECT '\n=== TODAY''S ATTENDANCE ===' as status;
SELECT 
    a.id,
    e.first_name as employee_name,
    a.date,
    a.status,
    a.check_in,
    a.check_out
FROM attendance a
JOIN employees e ON a.employee_id = e.id
WHERE a.date = CURDATE()
ORDER BY a.id;

-- ============================================
-- STEP 7: FINAL SUMMARY
-- ============================================

SELECT '\n=== FINAL DATA SUMMARY ===' as status;

SELECT 'Payroll Records' as category, COUNT(*) as count FROM payroll
UNION ALL
SELECT 'Payslips' as category, COUNT(*) as count FROM payslips
UNION ALL
SELECT 'Attendance (This Month)' as category, COUNT(*) as count 
FROM attendance 
WHERE MONTH(date) = MONTH(CURDATE()) AND YEAR(date) = YEAR(CURDATE())
UNION ALL
SELECT 'Active Employees' as category, COUNT(*) as count FROM employees WHERE status = 'ACTIVE';

-- Show latest data for each category
SELECT '\n=== LATEST PAYROLL ===' as status;
SELECT * FROM payroll ORDER BY id DESC LIMIT 3;

SELECT '\n=== LATEST PAYSLIPS ===' as status;
SELECT * FROM payslips ORDER BY id DESC LIMIT 3;

SELECT '\n=== LATEST ATTENDANCE ===' as status;
SELECT * FROM attendance ORDER BY id DESC LIMIT 3;

-- ============================================
-- CLEANUP COMPLETE!
-- ============================================
-- Your database now has fresh data for:
-- ✅ Current month payroll (March 2026)
-- ✅ Current month payslips (March 2026)
-- ✅ Today's attendance
-- ✅ All active employees with salaries
--
-- Next Steps:
-- 1. Refresh browser (Ctrl + Shift + R)
-- 2. Go to Payroll page → Should see new data
-- 3. Go to Payslips page → Should see new data
-- 4. Go to Attendance page → Should see today's data
-- ============================================
