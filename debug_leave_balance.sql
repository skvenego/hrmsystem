-- ============================================
-- WHERE DATA IS STORED - Employee ID: 16
-- ============================================

-- 1. APPROVED LEAVES (Source of truth for USED leaves)
-- This is where your 1 approved leave is stored
SELECT 
    'APPROVED LEAVES' as data_source,
    id,
    employee_id,
    leave_type,
    start_date,
    end_date,
    total_days,
    paid_days,
    unpaid_days,
    status
FROM leaves 
WHERE employee_id = 16 
  AND status = 'APPROVED';

-- 2. LEAVE_BALANCES (Cached/Snapshot - can be wrong!)
-- This is where the WRONG "Used: 0.0" might be cached
SELECT 
    'LEAVE_BALANCES TABLE' as data_source,
    *
FROM leave_balances 
WHERE employee_id = 16;

-- 3. PAYROLL (Where Attendance Summary gets data from)
-- This is why "Paid Leave: 1.0" shows correctly
SELECT 
    'PAYROLL TABLE' as data_source,
    id,
    employee_id,
    month,
    year,
    paid_leave_days,
    unpaid_leave_days,
    present_days,
    absent_days
FROM payroll 
WHERE employee_id = 16 
  AND month = 4 
  AND year = 2026;

-- 4. ATTENDANCE (Individual daily records)
SELECT 
    'ATTENDANCE TABLE' as data_source,
    id,
    employee_id,
    date,
    status
FROM attendance 
WHERE employee_id = 16 
  AND MONTH(date) = 4 
  AND YEAR(date) = 2026;
