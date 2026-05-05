-- DEBUG: Check all leaves for Anubhav Sharma (employee 16)
USE hrmsystem;

SELECT 
    l.id,
    l.employee_id,
    e.first_name,
    e.last_name,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    l.status,
    l.is_half_day,
    l.leave_type,
    l.created_at
FROM leaves l
JOIN employees e ON l.employee_id = e.id
WHERE l.employee_id = 16 
  AND l.status = 'APPROVED'
ORDER BY l.start_date;

-- Check what payroll data exists
SELECT 
    p.id,
    p.employee_id,
    p.month,
    p.year,
    p.paid_leave_days,
    p.unpaid_leave_days,
    p.absent_leave_deduction,
    p.total_deductions,
    p.net_salary
FROM payroll p
WHERE p.employee_id = 16
ORDER BY p.year, p.month;

-- Check what payslip data exists
SELECT 
    ps.id,
    ps.employee_id,
    ps.month_year,
    ps.salary_month,
    ps.salary_year,
    ps.paid_leave_days,
    ps.unpaid_leave_days,
    ps.absent_leave_deduction,
    ps.total_deduction,
    ps.net_salary
FROM payslips ps
WHERE ps.employee_id = 16
ORDER BY ps.salary_year, ps.salary_month;
