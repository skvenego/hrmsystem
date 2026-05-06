-- Check leave data to verify paidDays/unpaidDays are stored correctly
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
WHERE status = 'APPROVED'
ORDER BY id DESC
LIMIT 10;
