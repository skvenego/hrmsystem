-- Add half_days and absent_leave_deduction columns to payslips table for attendance-based salary deduction
-- Run only if columns don't exist (e.g. when using Flyway with existing Hibernate-managed schema, skip or run manually)
ALTER TABLE payslips ADD COLUMN half_days INT DEFAULT 0;
ALTER TABLE payslips ADD COLUMN absent_leave_deduction DECIMAL(10, 2) DEFAULT 0;
