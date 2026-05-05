-- Check May 2026 payroll for all employees
USE hrm_db;

-- View all May 2026 payrolls
SELECT 
    p.id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    p.employee_id,
    p.basic_salary,
    p.provident_fund,
    p.tax,
    p.insurance,
    p.other_deductions,
    p.total_deductions,
    p.gross_salary,
    p.net_salary,
    p.status
FROM payroll p
JOIN employees e ON p.employee_id = e.id
WHERE p.month = 5 AND p.year = 2026
ORDER BY p.employee_id;

-- Check if any May 2026 payrolls exist
SELECT COUNT(*) as may_payroll_count 
FROM payroll 
WHERE month = 5 AND year = 2026;

-- Expected calculation formula check for each employee
SELECT 
    e.id as employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    e.basic_salary,
    -- Expected PF (12% of basic or stored value)
    COALESCE(e.pf, e.basic_salary * 0.12) as expected_pf,
    -- Expected tax (stored or calculated)
    COALESCE(e.tax, 0) as expected_tax,
    -- Daily rate for attendance deductions
    ROUND(e.basic_salary / 26, 2) as daily_rate
FROM employees e
WHERE e.status = 'ACTIVE'
ORDER BY e.id;
