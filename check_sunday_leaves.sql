-- Check Sunday-related leave data in database
-- This will show which leaves include Sundays and how they're stored

-- Check leaves that span across Sundays
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
    DAYNAME(start_date) as start_day_name,
    DAYNAME(end_date) as end_day_name,
    -- Calculate actual days including Sundays
    DATEDIFF(end_date, start_date) + 1 as calendar_days,
    -- Count Sundays in the leave period
    (SELECT COUNT(*) 
     FROM (
         SELECT DATE_ADD(start_date, INTERVAL seq DAY) as check_date
         FROM (SELECT 0 as seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 
               UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) seq
         WHERE DATE_ADD(start_date, INTERVAL seq DAY) <= end_date
     ) dates
     WHERE DAYNAME(DATE_ADD(start_date, INTERVAL seq DAY)) = 'Sunday'
    ) as sunday_count
FROM leaves 
WHERE status = 'APPROVED'
AND (start_date <= '2026-05-31' AND end_date >= '2026-05-01')
ORDER BY employee_id, start_date;

-- Check specific May 2026 leave that user mentioned
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
    DAYNAME(start_date) as start_day_name,
    DAYNAME(end_date) as end_day_name,
    -- Show each day in the leave period
    CASE 
        WHEN DAYNAME(start_date) = 'Sunday' THEN 'Starts on Sunday'
        WHEN DAYNAME(end_date) = 'Sunday' THEN 'Ends on Sunday'
        ELSE 'No Sunday on boundaries'
    END as sunday_boundary_check
FROM leaves 
WHERE status = 'APPROVED'
AND leave_type = 'SICK'
AND start_date = '2026-05-01'
AND end_date = '2026-05-04';
