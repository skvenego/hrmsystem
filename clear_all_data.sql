-- Clear all data from HRMS database for fresh start
-- Run these queries in order to avoid foreign key constraint errors

USE hrmsystem;

-- Clear leaves (depends on employees)
DELETE FROM leaves;

-- Clear attendance (depends on employees)
DELETE FROM attendance;

-- Clear leave balances (depends on employees)
DELETE FROM leave_balances;

-- Clear payslips (depends on employees and payroll)
DELETE FROM payslips;

-- Clear payroll (depends on employees)
DELETE FROM payroll;

-- Clear audit logs
DELETE FROM audit_logs;

-- Reset auto-increment counters (optional - resets IDs to start from 1)
ALTER TABLE leaves AUTO_INCREMENT = 1;
ALTER TABLE attendance AUTO_INCREMENT = 1;
ALTER TABLE leave_balances AUTO_INCREMENT = 1;
ALTER TABLE payslips AUTO_INCREMENT = 1;
ALTER TABLE payroll AUTO_INCREMENT = 1;
ALTER TABLE audit_logs AUTO_INCREMENT = 1;

-- Note: We are NOT clearing employees table as it contains employee master data
-- If you want to clear employees too, uncomment the following:
-- DELETE FROM employees;
-- ALTER TABLE employees AUTO_INCREMENT = 1;
