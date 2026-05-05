-- FIX: Correct the paid/unpaid days for pending leave ID 6
-- For a 3-day leave with 1.0 remaining balance:
-- Should be: 1 paid + 2 unpaid (NOT 0.5 + 2.5)

USE hrm_db;

-- First, check the current balance for employee 16
SELECT 
    employee_id,
    total_earned_leaves,
    used_leaves,
    available_leaves
FROM leave_balances 
WHERE employee_id = 16;

-- Check all approved leaves that consume balance
SELECT 
    id,
    start_date,
    end_date,
    paid_days,
    status
FROM leaves 
WHERE employee_id = 16 
AND status = 'APPROVED'
ORDER BY start_date;

-- FIX: Update leave ID 6 to correct paid/unpaid split
-- Based on: 3 days total, 1.0 available balance after approved leaves
-- Should be: 1 paid + 2 unpaid
UPDATE leaves 
SET paid_days = 1.0,
    unpaid_days = 2.0
WHERE id = 6;

-- Verify the fix
SELECT id, leave_type, start_date, end_date, total_days, paid_days, unpaid_days, status
FROM leaves 
WHERE id = 6;
