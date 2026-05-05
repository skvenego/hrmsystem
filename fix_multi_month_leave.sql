-- ============================================
-- FIX EXISTING MULTI-MONTH LEAVE DATA
-- For: Anubhav Sharma (ID: 16), Leave Apr 30 - May 3
-- ============================================

-- First, let's check the current leave data
SELECT 
    id,
    employee_id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    status
FROM leaves 
WHERE employee_id = 16 
  AND status = 'APPROVED'
  AND start_date = '2026-04-30';

-- ============================================
-- CORRECT CALCULATION:
-- Apr 30 (Thu) = 1 day
-- May 1 (Fri) = 1 day  
-- May 2 (Sat) = 1 day
-- May 3 (Sun) = EXCLUDED
-- Total = 3 days (NOT 4)
-- ============================================

-- Update the leave with correct values
UPDATE leaves 
SET 
    total_days = 3.0,
    paid_days = 3.0,  -- Assuming sufficient balance
    unpaid_days = 0.0
WHERE employee_id = 16 
  AND status = 'APPROVED'
  AND start_date = '2026-04-30';

-- Verify the fix
SELECT 
    id,
    employee_id,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days
FROM leaves 
WHERE employee_id = 16 
  AND status = 'APPROVED';

-- ============================================
-- FIX PAYROLL DATA FOR APRIL 2026
-- ============================================

-- Check current payroll data
SELECT * FROM payroll 
WHERE employee_id = 16 
  AND month = 4 
  AND year = 2026;

-- Update April payroll (April portion = 1 day)
UPDATE payroll 
SET 
    paid_leave_days = 1.0,
    unpaid_leave_days = 0.0
WHERE employee_id = 16 
  AND month = 4 
  AND year = 2026;

-- Insert May payroll if not exists (May portion = 2 days)
-- Note: Adjust based on your actual payroll structure
INSERT INTO payroll (
    employee_id, month, year, paid_leave_days, unpaid_leave_days, 
    present_days, absent_days, basic_salary, hra, da, 
    total_salary, net_salary, payment_status
)
SELECT 
    16, 5, 2026, 2.0, 0.0,
    present_days, absent_days, basic_salary, hra, da,
    total_salary, net_salary, 'PENDING'
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026
ON DUPLICATE KEY UPDATE 
    paid_leave_days = 2.0,
    unpaid_leave_days = 0.0;

-- ============================================
-- VERIFY ALL DATA
-- ============================================

-- Check all leaves for employee
SELECT 
    'LEAVES' as data_type,
    id, start_date, end_date, total_days, paid_days, unpaid_days
FROM leaves 
WHERE employee_id = 16 AND status = 'APPROVED';

-- Check payroll data
SELECT 
    'PAYROLL' as data_type,
    month, year, paid_leave_days, unpaid_leave_days
FROM payroll 
WHERE employee_id = 16 
  AND ((month = 4 AND year = 2026) OR (month = 5 AND year = 2026));
