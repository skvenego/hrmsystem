-- Change paid_leave_days column from INTEGER to DOUBLE to support decimal values (e.g., 0.5 for half-day leaves)
ALTER TABLE payslips MODIFY COLUMN paid_leave_days DOUBLE;
