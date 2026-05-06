-- Check ALL approved leaves to see the total
SELECT 
    employee_id,
    COUNT(*) as total_leave_records,
    SUM(paid_days) as total_paid_days,
    SUM(unpaid_days) as total_unpaid_days,
    SUM(total_days) as total_days
FROM leaves
WHERE status = 'APPROVED'
GROUP BY employee_id
ORDER BY employee_id;

-- Show individual leave records for each employee
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
ORDER BY employee_id, start_date;
