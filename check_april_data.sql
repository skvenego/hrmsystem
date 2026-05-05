-- Check April 2026 data for Anubhav (Employee 16)
USE hrm_db;

-- Check all approved leaves for April 2026
SELECT 
    id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    status
FROM leaves 
WHERE employee_id = 16 
AND status = 'APPROVED'
AND (
    (start_date BETWEEN '2026-04-01' AND '2026-04-30')
    OR (end_date BETWEEN '2026-04-01' AND '2026-04-30')
    OR (start_date <= '2026-04-01' AND end_date >= '2026-04-30')
)
ORDER BY start_date;

-- Check attendance records for April 2026
SELECT 
    date,
    status,
    check_in_time,
    check_out_time,
    working_hours
FROM attendance 
WHERE employee_id = 16 
AND date BETWEEN '2026-04-01' AND '2026-04-30'
ORDER BY date;

-- Check current payroll for April 2026
SELECT 
    id,
    month,
    year,
    basic_salary,
    gross_salary,
    net_salary
FROM payroll 
WHERE employee_id = 16 
AND month = 4 
AND year = 2026;
