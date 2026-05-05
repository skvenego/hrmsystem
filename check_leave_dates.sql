-- Check actual leave dates for Employee 16
USE hrm_db;

SELECT id, start_date, end_date, paid_days, unpaid_days, total_days, status, leave_type,
       DATEDIFF(end_date, start_date) + 1 as calculated_total_days
FROM leaves 
WHERE employee_id = 16;

-- Check if there are any leaves on April 28
SELECT id, start_date, end_date, status 
FROM leaves 
WHERE employee_id = 16 
AND '2026-04-28' BETWEEN start_date AND end_date;
