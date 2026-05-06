-- Debug query to check paid leave days calculation
-- This shows the actual leave data from the database
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
WHERE status = 'APPROVED'
ORDER BY id DESC
LIMIT 20;
