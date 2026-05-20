-- =====================================================
-- FIX DATA - FINAL CORRECTED QUERIES
-- =====================================================

-- 1. Verify Leave 5 is fixed (should show paid_days=2, unpaid_days=2)
SELECT 
    id, employee_id, start_date, end_date, total_days, 
    paid_days, unpaid_days, status, leave_type
FROM leaves 
WHERE id = 5;

-- 2. Check current leave balance for Anubhav Sharma (ID: 16)
SELECT * FROM leave_balances WHERE employee_id = 16;

-- 3. FIX leave balance for Anubhav Sharma (ID: 16) - April 2026, Cycle 1
-- As of April 2026:
-- - Earned: 4 months * 1.5 = 6.0 days (Jan, Feb, Mar, Apr)
-- - Used: Leave 4 (2 days) + Leave 5 Apr portion (2 days) = 4 days
-- - Remaining: 6 - 4 = 2 days
UPDATE leave_balances 
SET 
    total_earned_leaves = 6,
    Used Leaves_leaves = 4.0,
    unpaid_leaves = 0,
    cycle = 1,
    year = 2026
WHERE employee_id = 16;

-- 4. Verify the fix
SELECT 
    employee_id,
    total_earned_leaves,
    Used Leaves_leaves,
    (total_earned_leaves - Used Leaves_leaves) as available_leaves,
    unpaid_leaves,
    cycle,
    year
FROM leave_balances 
WHERE employee_id = 16;

-- 5. Check all leaves for employee 16
SELECT 
    l.id,
    l.leave_type,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    l.status,
    CASE 
        WHEN l.paid_days = l.total_days THEN 'FULL PAID'
        WHEN l.paid_days = 0 THEN 'FULL UNPAID'
        ELSE 'PARTIAL'
    END as payment_status
FROM leaves l
WHERE l.employee_id = 16
ORDER BY l.start_date;

-- 6. Check if any payslips exist for April 2026
SELECT id, employee_id, month_year, salary_month, salary_year, 
       paid_leave_days, unpaid_leave_days, net_salary
FROM payslips 
WHERE employee_id = 16 AND salary_month = 4 AND salary_year = 2026;

-- 7. Delete April payslip if exists (force regeneration with new code)
DELETE FROM payslips 
WHERE employee_id = 16 AND salary_month = 4 AND salary_year = 2026;

-- 8. Also check May payslip
SELECT id, month_year, paid_leave_days, unpaid_leave_days 
FROM payslips 
WHERE employee_id = 16 AND salary_month = 5 AND salary_year = 2026;

-- 9. Delete May payslip too (if needed)
-- DELETE FROM payslips 
-- WHERE employee_id = 16 AND salary_month = 5 AND salary_year = 2026;
