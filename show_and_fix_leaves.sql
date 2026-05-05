-- SHOW ACTUAL LEAVE DATA FOR EMPLOYEE 16
USE hrm_db;

-- 1. Show all leaves for employee 16
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
    DATEDIFF(end_date, start_date) + 1 as calculated_days
FROM leaves 
WHERE employee_id = 16
ORDER BY start_date;

-- 2. Show attendance for April 2026
SELECT 
    id,
    employee_id,
    date,
    status,
    check_in_time,
    check_out_time,
    working_hours
FROM attendance 
WHERE employee_id = 16 
AND MONTH(date) = 4 
AND YEAR(date) = 2026
ORDER BY date;

-- 3. FIX: If you want April 28 to be included as a leave day,
-- you need to either:
--    a) Extend leave id 4 to end on Apr 28, OR
--    b) Create a new leave for Apr 28

-- Option A: Extend leave id 4 (Apr 26-27) to include Apr 28
-- UPDATE leaves 
-- SET end_date = '2026-04-28', 
--     total_days = 3,
--     paid_days = 3
-- WHERE id = 4;

-- Option B: Create a single day leave for Apr 28 (if it's unpaid)
-- INSERT INTO leaves (employee_id, leave_type, start_date, end_date, total_days, paid_days, unpaid_days, status, reason, applied_date)
-- VALUES (16, 'CASUAL', '2026-04-28', '2026-04-28', 1, 0, 1, 'APPROVED', 'Missing day', NOW());

-- 4. After fixing, verify the data
-- SELECT * FROM leaves WHERE employee_id = 16 ORDER BY start_date;
