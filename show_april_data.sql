-- SHOW APRIL 2026 DATA FOR ANUBHAV SHARMA (ID 16)
USE hrmsystem;

-- Get employee info
SELECT 
    e.id,
    e.first_name,
    e.last_name,
    e.joining_date,
    e.probation_period_months,
    e.basic_salary
FROM employees e
WHERE e.id = 16;

-- Show ALL approved leaves for Anubhav Sharma
SELECT 
    l.id,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    l.is_half_day,
    l.status,
    l.leave_type
FROM leaves l
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED'
ORDER BY l.start_date;

-- Calculate what SHOULD be shown for April 2026
-- April is Cycle 1 (Jan-Jun), Month 4
-- Earned by April: 4 months × 1.5 = 6.0 days
-- Used up to April: Sum of paid_days for leaves ending on or before April 30, 2026 AND within Cycle 1

SELECT 
    'CYCLE 1 (Jan-Jun 2026)' as cycle,
    '2026-01-01' as cycle_start,
    '2026-06-30' as cycle_end,
    '2026-04-30' as april_end,
    SUM(CASE 
        WHEN l.end_date <= '2026-04-30' 
             AND l.end_date >= '2026-01-01'  -- Within Cycle 1
             AND l.end_date <= '2026-06-30'  -- Within Cycle 1
        THEN l.paid_days 
        ELSE 0 
    END) as used_leaves_up_to_april,
    SUM(CASE 
        WHEN l.start_date >= '2026-04-01' 
             AND l.end_date <= '2026-04-30'  -- Fully within April
        THEN l.paid_days 
        ELSE 0 
    END) as used_leaves_in_april_only,
    6.0 as earned_by_april,
    6.0 - SUM(CASE 
        WHEN l.end_date <= '2026-04-30' 
             AND l.end_date >= '2026-01-01'
             AND l.end_date <= '2026-06-30'
        THEN l.paid_days 
        ELSE 0 
    END) as remaining_by_april
FROM leaves l
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED';

-- Show payslip data for April 2026
SELECT 
    ps.id,
    ps.month_year,
    ps.salary_month,
    ps.salary_year,
    ps.total_earned_leaves,
    ps.used_leaves,
    ps.available_leaves,
    ps.paid_leave_days,
    ps.unpaid_leave_days
FROM payslips ps
WHERE ps.employee_id = 16
  AND ps.salary_month = 4
  AND ps.salary_year = 2026;

-- Show payroll data for April 2026  
SELECT 
    p.id,
    p.month,
    p.year,
    p.paid_leave_days,
    p.unpaid_leave_days,
    p.leave_days
FROM payroll p
WHERE p.employee_id = 16
  AND p.month = 4
  AND p.year = 2026;
