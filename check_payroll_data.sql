-- ============================================
-- CHECK PAYROLL DATA FOR ANUBHAV SHARMA (ID: 16)
-- ============================================

-- 1. Check all payroll records for employee 16
SELECT 
    'ALL PAYROLL RECORDS' as section,
    id,
    employee_id,
    month,
    year,
    paid_leave_days,
    unpaid_leave_days,
    present_days,
    absent_days,
    basic_salary,
    net_salary
FROM payroll 
WHERE employee_id = 16
ORDER BY year DESC, month DESC;

-- 2. Check April 2026 payroll specifically
SELECT 
    'APRIL 2026 PAYROLL' as section,
    id,
    employee_id,
    paid_leave_days,
    unpaid_leave_days,
    present_days,
    absent_days,
    basic_salary,
    hra,
    da,
    other_allowance,
    pf,
    tax,
    insurance,
    total_salary,
    net_salary,
    payment_status
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026;

-- 3. Check May 2026 payroll specifically
SELECT 
    'MAY 2026 PAYROLL' as section,
    id,
    employee_id,
    paid_leave_days,
    unpaid_leave_days,
    present_days,
    absent_days,
    basic_salary,
    net_salary
FROM payroll 
WHERE employee_id = 16 AND month = 5 AND year = 2026;

-- 4. Check all approved leaves for employee 16
SELECT 
    'APPROVED LEAVES' as section,
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
WHERE employee_id = 16 AND status = 'APPROVED'
ORDER BY start_date;

-- 5. Check leave balance
SELECT 
    'LEAVE BALANCE' as section,
    id,
    employee_id,
    total_earned_leaves,
    Used Leaves_leaves,
    (total_earned_leaves - Used Leaves_leaves) as remaining_leaves,
    year,
    cycle
FROM leave_balances 
WHERE employee_id = 16;
