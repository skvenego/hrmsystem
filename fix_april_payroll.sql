-- ============================================
-- FIX APRIL 2026 PAYROLL FOR ANUBHAV SHARMA (ID: 16)
-- ============================================

-- Check current payroll data
SELECT * FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026;

-- Update April payroll with correct paid leave days (2 days: Apr 29 + Apr 30)
UPDATE payroll 
SET 
    paid_leave_days = 2.0,
    unpaid_leave_days = 0.0,
    present_days = 24.0  -- Assuming 26 working days - 2 paid leave
WHERE employee_id = 16 
  AND month = 4 
  AND year = 2026;

-- Insert/Update May payroll (2 days: May 1 + May 3, May 2 Sunday excluded)
INSERT INTO payroll (
    employee_id, month, year, basic_salary, hra, da, other_allowance,
    paid_leave_days, unpaid_leave_days, present_days, absent_days,
    pf, tax, insurance, total_salary, net_salary, payment_status
)
SELECT 
    16, 5, 2026, basic_salary, hra, da, other_allowance,
    2.0, 0.0, 24.0, 0.0,  -- 2 paid leave days in May
    pf, tax, insurance, total_salary, net_salary, 'PENDING'
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026
ON DUPLICATE KEY UPDATE 
    paid_leave_days = 2.0,
    unpaid_leave_days = 0.0,
    present_days = 24.0;

-- Verify fixes
SELECT 
    'APRIL' as month,
    paid_leave_days,
    unpaid_leave_days,
    present_days
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026
UNION ALL
SELECT 
    'MAY' as month,
    paid_leave_days,
    unpaid_leave_days,
    present_days
FROM payroll 
WHERE employee_id = 16 AND month = 5 AND year = 2026;
