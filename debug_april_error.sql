-- DEBUG APRIL 2026 ERROR - Show exactly what data exists
USE hrmsystem;

-- Get Anubhav Sharma (ID 16) info
SELECT 
    e.id,
    CONCAT(e.first_name, ' ', e.last_name) as name,
    e.joining_date,
    e.probation_period_months,
    DATE_ADD(e.joining_date, INTERVAL e.probation_period_months MONTH) as probation_end_date
FROM employees e
WHERE e.id = 16;

-- Show ALL approved leaves for Anubhav with cycle info
SELECT 
    l.id,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    l.is_half_day,
    -- Determine which cycle this leave belongs to
    CASE 
        WHEN MONTH(l.end_date) BETWEEN 1 AND 6 THEN 'CYCLE 1 (Jan-Jun)'
        ELSE 'CYCLE 2 (Jul-Dec)'
    END as cycle,
    -- Check if leave ends within April 2026
    CASE 
        WHEN l.end_date <= '2026-04-30' AND l.end_date >= '2026-01-01' THEN 'ENDS IN APRIL/CYCLE 1'
        ELSE 'ENDS AFTER APRIL'
    END as april_status
FROM leaves l
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED'
ORDER BY l.start_date;

-- Calculate what SHOULD be the values for April 2026
SELECT 
    'APRIL 2026 CALCULATION' as description,
    -- Earned leaves by April (4 months × 1.5 = 6.0, capped at 9)
    LEAST(4 * 1.5, 9.0) as earned_by_april,
    
    -- Used Leaves UP TO April (cycle 1, ending on or before April 30)
    SUM(CASE 
        WHEN l.end_date <= '2026-04-30' 
             AND l.end_date >= '2026-01-01'  -- Within Cycle 1
             AND l.end_date <= '2026-06-30'  -- Within Cycle 1
        THEN l.paid_days 
        ELSE 0 
    END) as Used Leaves_up_to_april,
    
    -- Used Leaves IN April only (fully within April)
    SUM(CASE 
        WHEN l.start_date >= '2026-04-01' 
             AND l.end_date <= '2026-04-30'
        THEN l.paid_days 
        ELSE 0 
    END) as Used Leaves_in_april_only,
    
    -- Remaining should be: Earned - Used up to April
    LEAST(4 * 1.5, 9.0) - SUM(CASE 
        WHEN l.end_date <= '2026-04-30' 
             AND l.end_date >= '2026-01-01'
             AND l.end_date <= '2026-06-30'
        THEN l.paid_days 
        ELSE 0 
    END) as should_be_remaining,
    
    -- What payslip shows vs what it SHOULD show
    CONCAT('Payslip shows Remaining: 4.0, Should be: ', 
        LEAST(4 * 1.5, 9.0) - SUM(CASE 
            WHEN l.end_date <= '2026-04-30' 
                 AND l.end_date >= '2026-01-01'
                 AND l.end_date <= '2026-06-30'
            THEN l.paid_days 
            ELSE 0 
        END)) as comparison

FROM leaves l
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED';

-- Check what payslip table actually stored
SELECT 
    ps.id,
    ps.month_year,
    ps.salary_month,
    ps.salary_year,
    ps.total_earned_leaves,
    ps.Used Leaves_leaves,
    ps.available_leaves,
    CONCAT('Stored Used: ', ps.Used Leaves_leaves, ', Stored Remaining: ', ps.available_leaves) as stored_values,
    CONCAT('Mismatch: Earned(6.0) - Used(', ps.Used Leaves_leaves, ') = ', 6.0 - ps.Used Leaves_leaves, ' but shows ', ps.available_leaves) as mismatch_check
FROM payslips ps
WHERE ps.employee_id = 16
  AND ps.salary_month = 4
  AND ps.salary_year = 2026;

-- Debug: Check if there's a leave that spans months causing calculation error
SELECT 
    l.id,
    l.start_date,
    l.end_date,
    l.paid_days,
    l.unpaid_days,
    -- Days in April
    DATEDIFF(
        LEAST(l.end_date, '2026-04-30'),
        GREATEST(l.start_date, '2026-04-01')
    ) + 1 as days_in_april,
    -- Total days
    DATEDIFF(l.end_date, l.start_date) + 1 as total_days,
    -- Proportional paid days for April
    l.paid_days * (
        (DATEDIFF(LEAST(l.end_date, '2026-04-30'), GREATEST(l.start_date, '2026-04-01')) + 1) 
        / (DATEDIFF(l.end_date, l.start_date) + 1)
    ) as proportional_paid_in_april
FROM leaves l
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED'
  AND l.start_date <= '2026-04-30'
  AND l.end_date >= '2026-04-01'
ORDER BY l.start_date;
