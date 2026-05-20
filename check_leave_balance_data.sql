-- ============================================
-- CHECK LEAVE BALANCE DATA FOR EMPLOYEE
-- Employee: Anubhav Sharma (ID: 16)
-- ============================================

-- 1. Check leave_balances table (stored balance)
SELECT 
    lb.id,
    lb.employee_id,
    e.first_name,
    e.last_name,
    lb.year,
    lb.cycle,
    lb.total_leaves as total_earned,
    lb.Used Leaves_leaves as Used Leaves,
    lb.remaining_leaves as remaining,
    lb.unpaid_leaves as unpaid,
    lb.updated_at
FROM leave_balances lb
JOIN employees e ON lb.employee_id = e.id
WHERE lb.employee_id = 16
ORDER BY lb.year DESC, lb.cycle DESC;

-- 2. Check all approved leaves for this employee (Cycle 1: Jan-Jun 2026)
SELECT 
    l.id,
    l.employee_id,
    l.leave_type,
    l.start_date,
    l.end_date,
    l.total_days,
    l.paid_days,
    l.unpaid_days,
    l.status,
    l.applied_at
FROM leaves l
WHERE l.employee_id = 16
  AND l.status = 'APPROVED'
  AND l.end_date >= '2026-01-01' 
  AND l.end_date <= '2026-06-30'
ORDER BY l.start_date;

-- 3. Check attendance data for April 2026
SELECT 
    a.id,
    a.employee_id,
    a.date,
    a.status,
    a.is_paid_leave,
    a.is_unpaid_leave
FROM attendance a
WHERE a.employee_id = 16
  AND a.date >= '2026-04-01' 
  AND a.date <= '2026-04-30'
ORDER BY a.date;

-- 4. Check payslip data for April 2026
SELECT 
    p.id,
    p.employee_id,
    p.month,
    p.year,
    p.paid_leave_days,
    p.unpaid_leave_days,
    p.present_days,
    p.absent_days
FROM payroll p
WHERE p.employee_id = 16
  AND p.month = 4 
  AND p.year = 2026;

-- 5. Check payslip_absent_dates for detailed breakdown
SELECT 
    pad.id,
    pad.payslip_id,
    pad.absent_date,
    pad.is_paid,
    pad.leave_id
FROM payslip_absent_dates pad
JOIN payroll p ON pad.payslip_id = p.id
WHERE p.employee_id = 16
  AND p.month = 4 
  AND p.year = 2026;

-- 6. Calculate Used Leaves leaves manually (excluding Sundays)
-- This should match what the Java code calculates
SELECT 
    l.id,
    l.start_date,
    l.end_date,
    l.total_days,
    -- Days excluding Sundays (manual calculation)
    (DATEDIFF(l.end_date, l.start_date) + 1) - 
    (FLOOR((DATEDIFF(l.end_date, l.start_date) + DAYOFWEEK(l.start_date) - 1) / 7)) as days_excluding_sundays,
    l.paid_days,
    l.unpaid_days
FROM leaves l
WHERE l.employee_id = 16
  AND l.status = 'APPROVED'
  AND l.end_date >= '2026-01-01' 
  AND l.end_date <= '2026-06-30';
