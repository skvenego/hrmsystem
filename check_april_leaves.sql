-- Check leaves for Anubhav Sharma (employee 16) in April 2026
USE hrmsystem;

-- Check approved leaves in April 2026
SELECT 
    l.id,
    l.employee_id,
    e.first_name,
    e.last_name,
    l.start_date,
    l.end_date,
    l.paid_days,
    l.unpaid_days,
    l.status,
    l.is_half_day
FROM leaves l
JOIN employees e ON l.employee_id = e.id
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED'
  AND (
    (l.start_date BETWEEN '2026-04-01' AND '2026-04-30')
    OR (l.end_date BETWEEN '2026-04-01' AND '2026-04-30')
    OR (l.start_date <= '2026-04-01' AND l.end_date >= '2026-04-30')
  )
ORDER BY l.start_date;

-- Check attendance records for April 2026
SELECT 
    a.id,
    a.employee_id,
    a.date,
    a.status,
    a.is_half_day
FROM attendance a
WHERE a.employee_id = 16 
  AND a.date BETWEEN '2026-04-01' AND '2026-04-30'
ORDER BY a.date;

-- Check payroll for April 2026
SELECT 
    p.id,
    p.employee_id,
    p.month,
    p.year,
    p.present_days,
    p.absent_days,
    p.leave_days,
    p.paid_leave_days,
    p.unpaid_leave_days,
    p.half_days,
    p.total_days,
    p.basic_salary,
    p.gross_salary,
    p.total_deductions,
    p.net_salary
FROM payroll p
WHERE p.employee_id = 16 
  AND p.month = 4 
  AND p.year = 2026;
