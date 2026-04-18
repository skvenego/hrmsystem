-- =====================================================
-- CLEANUP DUPLICATE PAYROLL RECORDS FOR ALL EMPLOYEES
-- =====================================================

-- Use the correct database
USE hrm_db;

-- Step 1: Find all duplicate payrolls (for review)
SELECT 
    employee_id, 
    month, 
    year, 
    COUNT(*) as duplicate_count
FROM payroll
GROUP BY employee_id, month, year
HAVING COUNT(*) > 1;

-- Step 2: Keep only the most recent payroll for each employee/month/year
-- Delete all older duplicates, keep the one with highest ID (most recent)
DELETE p1 FROM payroll p1
INNER JOIN payroll p2 
    ON p1.employee_id = p2.employee_id 
    AND p1.month = p2.month 
    AND p1.year = p2.year
    AND p1.id < p2.id;

-- Step 3: Verify no duplicates remain
SELECT 
    employee_id, 
    month, 
    year, 
    COUNT(*) as count
FROM payroll
GROUP BY employee_id, month, year
HAVING COUNT(*) > 1;

-- =====================================================
-- PREVENT FUTURE DUPLICATES (OPTIONAL - RUN AFTER CLEANUP)
-- =====================================================

-- Add unique constraint to prevent duplicates in future
-- Only run this after cleanup is complete!
-- ALTER TABLE payroll 
-- ADD CONSTRAINT uk_employee_month_year 
-- UNIQUE (employee_id, month, year);
