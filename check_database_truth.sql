-- Check database truth for leave calculations
-- This will show the actual values stored in the database

SELECT 
    employee_id,
    COUNT(*) as total_leaves,
    COALESCE(SUM(paid_days), 0) as total_paid_days,
    COALESCE(SUM(unpaid_days), 0) as total_unpaid_days,
    COALESCE(SUM(paid_days + unpaid_days), 0) as total_Used Leaves_days,
    MIN(start_date) as earliest_leave,
    MAX(end_date) as latest_leave
FROM leaves 
WHERE status = 'APPROVED' 
GROUP BY employee_id
ORDER BY employee_id;

-- Check monthly breakdown for current month (May 2026)
SELECT 
    employee_id,
    COUNT(*) as monthly_leaves,
    COALESCE(SUM(paid_days), 0) as monthly_paid_days,
    COALESCE(SUM(unpaid_days), 0) as monthly_unpaid_days,
    COALESCE(SUM(paid_days + unpaid_days), 0) as monthly_Used Leaves_days
FROM leaves 
WHERE status = 'APPROVED' 
AND MONTH(start_date) = 5 
AND YEAR(start_date) = 2026
GROUP BY employee_id
ORDER BY employee_id;

-- Check individual leave records for verification
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
    applied_date
FROM leaves 
WHERE status = 'APPROVED' 
ORDER BY employee_id, start_date;
