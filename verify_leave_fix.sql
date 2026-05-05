-- Verify the leave fix and check current state
USE hrm_db;

-- Check leave ID 6 current values
SELECT id, leave_type, start_date, end_date, total_days, paid_days, unpaid_days, status
FROM leaves WHERE id = 6;

-- Also verify all leaves for employee 16
SELECT id, leave_type, start_date, end_date, total_days, paid_days, unpaid_days, status
FROM leaves WHERE employee_id = 16 ORDER BY start_date;

-- If still incorrect, try direct update with explicit casting
-- UPDATE leaves SET paid_days = CAST(1.0 AS DECIMAL(10,2)), unpaid_days = CAST(2.0 AS DECIMAL(10,2)) WHERE id = 6;
