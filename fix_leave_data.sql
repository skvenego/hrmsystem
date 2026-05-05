-- =====================================================
-- FIX LEAVE DATA - Multi-month leave calculation fix
-- =====================================================

-- 1. Check current leave data for Anubhav Sharma (ID: 16)
SELECT 
    id,
    employee_id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    status,
    is_half_day
FROM leaves 
WHERE employee_id = 16 
ORDER BY start_date;

-- 2. FIX Leave 5 (Apr 29 - May 2) - Split by month:
--    Apr 29-30 = 2 days (April portion)
--    May 1-2 = 2 days (May portion)
-- 
-- If April has balance: Apr portion paid
-- If May has balance: May portion paid
-- 
-- Current fix: Set paid=2 (Apr portion only, assuming Apr balance used)
--              unpaid=2 (May portion - will be deducted from May salary)

UPDATE leaves 
SET 
    paid_days = 2.0,
    unpaid_days = 2.0,
    updated_at = NOW()
WHERE id = 5;  -- Leave 5: Apr 29 - May 2

-- 3. Verify the fix
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
WHERE id = 5;

-- 4. Fix all multi-month leaves with incorrect paid/unpaid calculation
-- This finds all leaves that span multiple months and recalculates

SELECT 
    l.id,
    l.employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    CASE 
        WHEN MONTH(l.start_date) != MONTH(l.end_date) THEN 'MULTI-MONTH'
        ELSE 'SINGLE-MONTH'
    END as leave_type_calc
FROM leaves l
JOIN employees e ON l.employee_id = e.id
WHERE l.status = 'APPROVED'
ORDER BY l.employee_id, l.start_date;

-- 5. Reset leave balance for all employees (recalculate from scratch)
-- WARNING: Only run this if you want to reset ALL leave balances

-- UPDATE leave_balances 
-- SET 
--     total_leaves = 0,
--     used_leaves = 0,
--     available_leaves = 0,
--     earned_leaves = 0,
--     updated_at = NOW();

-- 6. Recalculate leave balance for Anubhav Sharma (ID: 16) only
-- First, get total earned leaves (1.5 per month after probation)
SELECT 
    e.id,
    CONCAT(e.first_name, ' ', e.last_name) as name,
    e.joining_date,
    e.probation_period_months,
    DATE_ADD(e.joining_date, INTERVAL e.probation_period_months MONTH) as probation_end,
    DATEDIFF(NOW(), DATE_ADD(e.joining_date, INTERVAL e.probation_period_months MONTH)) / 30 as months_since_probation,
    LEAST(9, FLOOR(DATEDIFF(NOW(), DATE_ADD(e.joining_date, INTERVAL e.probation_period_months MONTH)) / 30) * 1.5) as earned_leaves_calc
FROM employees e
WHERE e.id = 16;

-- 7. Check current leave balance
SELECT * FROM leave_balances WHERE employee_id = 16;

-- 8. Manual fix for Anubhav Sharma balance (April 2026)
-- As of April 2026:
-- - Probation ended: Jan 5, 2026
-- - Months active: Jan, Feb, Mar, Apr = 4 months
-- - Earned: 4 * 1.5 = 6.0 days
-- - Used: Leave 4 (2 days) + Leave 5 Apr portion (2 days) = 4 days
-- - Remaining: 6 - 4 = 2.0 days

UPDATE leave_balances 
SET 
    total_leaves = 6.0,
    used_leaves = 4.0,
    available_leaves = 2.0,
    earned_leaves = 6.0,
    updated_at = NOW()
WHERE employee_id = 16;

-- 9. Verify final data
SELECT 
    lb.employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as name,
    lb.total_leaves,
    lb.used_leaves,
    lb.available_leaves,
    lb.earned_leaves
FROM leave_balances lb
JOIN employees e ON lb.employee_id = e.id
WHERE lb.employee_id = 16;
