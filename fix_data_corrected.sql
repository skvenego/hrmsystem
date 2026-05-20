-- =====================================================
-- FIX DATA - Corrected for actual schema
-- =====================================================

-- 1. Check leaves table structure first
DESCRIBE leaves;

-- 2. Fix Leave 5 (Apr 29 - May 2) - Set 2 paid, 2 unpaid
-- This splits the multi-month leave: Apr 29-30 = 2 days, May 1-2 = 2 days
UPDATE leaves 
SET 
    paid_days = 2.0,
    unpaid_days = 2.0
WHERE id = 5;

-- 3. Check what actually got updated
SELECT 
    id, employee_id, start_date, end_date, total_days, 
    paid_days, unpaid_days, status, leave_type
FROM leaves 
WHERE id = 5;

-- 4. Check payslips structure
DESCRIBE payslips;

-- 5. Delete old April payslip for Anubhav Sharma (ID: 16)
-- Run this after checking the column names
-- DELETE FROM payslips WHERE employee_id = 16 AND salary_month = 4 AND salary_year = 2026;

-- 6. Check leave_balances structure  
DESCRIBE leave_balances;

-- 7. Update leave balance if columns exist
-- Common column names might be: available_leaves, Used Leaves_leaves, earned_leaves
-- Or: total_leaves, consumed_leaves, remaining_leaves
-- Run DESCRIBE first to see actual columns

-- 8. Check current leave balance for employee 16
SELECT * FROM leave_balances WHERE employee_id = 16;

-- 9. Alternative: Check if there's a JSON column or separate table
SHOW TABLES LIKE '%leave%';
SHOW TABLES LIKE '%balance%';

-- 10. Get all leave data for employee 16 to verify
SELECT 
    l.id,
    l.employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
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
JOIN employees e ON l.employee_id = e.id
WHERE l.employee_id = 16
ORDER BY l.start_date;
