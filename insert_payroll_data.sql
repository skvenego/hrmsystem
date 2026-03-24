-- Insert sample payroll data for existing employees
INSERT INTO payroll (employee_id, basic_salary, hra, ta, da, gross_salary, 
                    tax, provident_fund, insurance, other_deductions, total_deductions,
                    net_salary, status, month, year)
SELECT 
    id as employee_id,
    basic_salary,
    basic_salary * 0.20 as hra,           -- 20% HRA
    basic_salary * 0.10 as ta,            -- 10% TA
    basic_salary * 0.05 as da,            -- 5% DA
    (basic_salary + (basic_salary * 0.35)) as gross_salary,
    basic_salary * 0.05 as tax,           -- 5% tax
    basic_salary * 0.03 as provident_fund, -- 3% PF
    0 as insurance,
    0 as other_deductions,
    (basic_salary * 0.08) as total_deductions,
    ((basic_salary + (basic_salary * 0.35)) - (basic_salary * 0.08)) as net_salary,
    'PENDING' as status,
    3 as month,  -- March
    2026 as year
FROM employees
WHERE id IN (2, 3, 5);  -- For specific employees

-- Show inserted records
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year, p.net_salary, p.status
FROM payroll p
JOIN employees e ON p.employee_id = e.id
ORDER BY p.id DESC LIMIT 5;
