-- IMMEDIATE FIX - DELETE DUPLICATES NOW!
-- Run this ONCE in MySQL Workbench

USE hrmsystem;

-- ============================================
-- STEP 1: SEE CURRENT DUPLICATES
-- ============================================
SELECT '=== BEFORE CLEANUP - PAYROLL DUPLICATES ===' as '';
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year 
FROM payroll p JOIN employee e ON p.employee_id = e.id 
ORDER BY p.employee_id, p.year, p.month;

-- ============================================
-- STEP 2: DELETE PAYROLL DUPLICATES
-- ============================================
SELECT '=== DELETING PAYROLL DUPLICATES ===' as '';

DELETE p1 FROM payroll p1
INNER JOIN payroll p2 
WHERE p1.id > p2.id 
  AND p1.employee_id = p2.employee_id 
  AND p1.month = p2.month 
  AND p1.year = p2.year;

-- ============================================
-- STEP 3: DELETE PAYSLIP DUPLICATES
-- ============================================
SELECT '=== DELETING PAYSLIP DUPLICATES ===' as '';

DELETE ps1 FROM payslip ps1
INNER JOIN payslip ps2 
WHERE ps1.id > ps2.id 
  AND ps1.employee_id = ps2.employee_id 
  AND ps1.month_year = ps2.month_year;

-- ============================================
-- STEP 4: ADD UNIQUE CONSTRAINTS
-- ============================================
SELECT '=== ADDING UNIQUE CONSTRAINTS ===' as '';

-- Add payroll unique constraint (ignore error if exists)
ALTER IGNORE TABLE payroll ADD UNIQUE KEY unique_employee_month_year (employee_id, month, year);

-- Add payslip unique constraint (ignore error if exists)
ALTER IGNORE TABLE payslip ADD UNIQUE KEY unique_employee_month_year (employee_id, month_year);

-- ============================================
-- STEP 5: VERIFY CLEANUP SUCCESSFUL
-- ============================================
SELECT '=== AFTER CLEANUP - SHOULD BE EMPTY ===' as '';
SELECT employee_id, month, year, COUNT(*) as count
FROM payroll
GROUP BY employee_id, month, year
HAVING count > 1;

SELECT '=== FINAL PAYROLL RECORDS (ONE PER EMPLOYEE) ===' as '';
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year, p.net_salary 
FROM payroll p JOIN employee e ON p.employee_id = e.id 
ORDER BY p.employee_id, p.year, p.month;

SELECT '=== DONE! REFRESH BROWSER (Ctrl+F5) ===' as '';
