-- Debug current approved leaves data
-- Check what's actually stored in the database

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
    final_paid_days,
    final_unpaid_days,
    applied_date
FROM leaves 
WHERE status = 'APPROVED' 
AND employee_id IN (
    SELECT id FROM employees ORDER BY id LIMIT 5
)
ORDER BY employee_id, start_date DESC
LIMIT 20;

-- Check totals per employee
SELECT 
    employee_id,
    COUNT(*) as total_approved_leaves,
    COALESCE(SUM(paid_days), 0) as total_paid_days,
    COALESCE(SUM(unpaid_days), 0) as total_unpaid_days,
    COALESCE(SUM(paid_days + unpaid_days), 0) as total_Used Leaves_days
FROM leaves 
WHERE status = 'APPROVED' 
GROUP BY employee_id
ORDER BY employee_id;
