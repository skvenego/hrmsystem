-- Compare database stored values vs actual working days (excluding Sundays)
-- This will show if database values need to be updated

-- Check May 2026 SICK leave from 1-4 May
SELECT 
    'DATABASE_VALUES' as source,
    id,
    employee_id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    -- Manual calculation excluding Sundays
    CASE 
        WHEN DAYNAME(start_date) = 'Sunday' THEN DATEDIFF(end_date, start_date)
        WHEN DAYNAME(end_date) = 'Sunday' THEN DATEDIFF(end_date, start_date)
        ELSE DATEDIFF(end_date, start_date) + 1 - 
            (SELECT COUNT(*) 
             FROM (
                 SELECT DATE_ADD(start_date, INTERVAL seq DAY) as check_date
                 FROM (SELECT 0 as seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 
                       UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) seq
                 WHERE DATE_ADD(start_date, INTERVAL seq DAY) <= end_date
             ) dates
             WHERE DAYNAME(DATE_ADD(start_date, INTERVAL seq DAY)) = 'Sunday'
            )
    END as actual_working_days,
    DAYNAME(start_date) as start_day,
    DAYNAME(end_date) as end_day
FROM leaves 
WHERE status = 'APPROVED'
AND leave_type = 'SICK'
AND start_date = '2026-05-01'
AND end_date = '2026-05-04'

UNION ALL

-- Show all approved leaves for current month with working day calculation
SELECT 
    'WORKING_DAYS_CHECK' as source,
    id,
    employee_id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    -- Calculate working days (excluding Sundays)
    DATEDIFF(end_date, start_date) + 1 - 
        (SELECT COUNT(*) 
         FROM (
             SELECT DATE_ADD(l.start_date, INTERVAL seq DAY) as check_date
             FROM leaves l2, (SELECT 0 as seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 
                   UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) seq
             WHERE l2.id = l.id AND DATE_ADD(l.start_date, INTERVAL seq DAY) <= l.end_date
         ) dates
         WHERE DAYNAME(DATE_ADD(l.start_date, INTERVAL seq DAY)) = 'Sunday'
        ) as working_days,
    DAYNAME(start_date) as start_day,
    DAYNAME(end_date) as end_day
FROM leaves l
WHERE status = 'APPROVED'
AND MONTH(start_date) = 5 AND YEAR(start_date) = 2026
ORDER BY source, employee_id, start_date;
