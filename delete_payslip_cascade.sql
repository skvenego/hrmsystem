-- Delete payslip with cascade for foreign keys
USE hrm_db;

-- First delete child records (absent dates)
DELETE FROM payslip_absent_dates 
WHERE payslip_id IN (SELECT id FROM payslips WHERE employee_id = 16 AND month_year = '2026-04');

-- Then delete the payslip
DELETE FROM payslips 
WHERE employee_id = 16 AND month_year = '2026-04';

-- Verify
SELECT COUNT(*) as remaining_payslips 
FROM payslips 
WHERE employee_id = 16 AND month_year = '2026-04';
