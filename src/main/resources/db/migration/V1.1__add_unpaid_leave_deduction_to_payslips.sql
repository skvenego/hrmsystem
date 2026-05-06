-- Add unpaid_leave_deduction column to payslips table
ALTER TABLE payslips ADD COLUMN unpaid_leave_deduction DECIMAL(10,2);