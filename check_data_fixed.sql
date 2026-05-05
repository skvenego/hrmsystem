-- ============================================
-- CORRECTED QUERIES - Check Actual Schema
-- Employee: Anubhav Sharma (ID: 16)
-- ============================================

-- 1. Check leave_balances table (use available columns)
DESCRIBE leave_balances;

-- Check what columns exist
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'leave_balances' AND TABLE_SCHEMA = DATABASE();

-- Now query with actual columns
SELECT 
    lb.*
FROM leave_balances lb
WHERE lb.employee_id = 16;

-- 2. Check leaves table
DESCRIBE leaves;

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
    l.created_at
FROM leaves l
WHERE l.employee_id = 16
  AND l.status = 'APPROVED'
ORDER BY l.start_date;

-- 3. Check attendance table
DESCRIBE attendance;

SELECT 
    a.id,
    a.employee_id,
    a.date,
    a.status
FROM attendance a
WHERE a.employee_id = 16
  AND MONTH(a.date) = 4 
  AND YEAR(a.date) = 2026
ORDER BY a.date;

-- 4. Check payroll table
DESCRIBE payroll;

SELECT 
    p.*
FROM payroll p
WHERE p.employee_id = 16
  AND p.month = 4 
  AND p.year = 2026;

-- 5. Check payslip_absent_dates table
DESCRIBE payslip_absent_dates;

-- Simple query
SELECT * FROM payslip_absent_dates LIMIT 5;

-- 6. MANUAL CALCULATION: Count approved leaves (excluding Sundays)
-- This is what Java SHOULD calculate
SELECT 
    l.id,
    l.start_date,
    l.end_date,
    l.total_days as stored_total,
    l.paid_days as stored_paid,
    -- Calculate fresh: days excluding Sundays
    (DATEDIFF(l.end_date, l.start_date) + 1) - 
    (FLOOR((DATEDIFF(l.end_date, l.start_date) + DAYOFWEEK(l.start_date) - 1) / 7)) as calculated_days,
    l.status
FROM leaves l
WHERE l.employee_id = 16
  AND l.status = 'APPROVED';

-- 7. SUMMARY: Total used leaves calculation
SELECT 
    COUNT(*) as total_approved_leaves,
    SUM(l.paid_days) as sum_paid_days_from_db,
    SUM(
        (DATEDIFF(l.end_date, l.start_date) + 1) - 
        (FLOOR((DATEDIFF(l.end_date, l.start_date) + DAYOFWEEK(l.start_date) - 1) / 7))
    ) as calculated_used_days
FROM leaves l
WHERE l.employee_id = 16
  AND l.status = 'APPROVED'
  AND l.end_date >= '2026-01-01' 
  AND l.end_date <= '2026-06-30';
