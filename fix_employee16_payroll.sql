-- Fix Employee 16 April 2026 Payroll Data
-- This updates the payroll record with correct total deductions including unpaid leave

USE hrm_db;

-- Check current payroll data for employee 16, April 2026
SELECT 
    id,
    employee_id,
    month,
    year,
    basic_salary,
    provident_fund,
    tax,
    insurance,
    other_deductions,
    total_deductions,
    gross_salary,
    net_salary
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026;

-- Update the payroll to have correct total deductions
-- PF (2448) + Tax (3400) + Insurance (170) + OtherDeductions (1308) = 7326
UPDATE payroll 
SET 
    total_deductions = 7326.00,
    net_salary = gross_salary - 7326.00,
    other_deductions = 1308.00
WHERE employee_id = 16 AND month = 4 AND year = 2026;

-- Delete the payslip to force regeneration with correct values
DELETE FROM payslips 
WHERE employee_id = 16 AND month_year = '2026-04';

-- Verify the fix
SELECT 
    id,
    employee_id,
    month,
    year,
    provident_fund,
    tax,
    insurance,
    other_deductions,
    total_deductions,
    gross_salary,
    net_salary
FROM payroll 
WHERE employee_id = 16 AND month = 4 AND year = 2026;
