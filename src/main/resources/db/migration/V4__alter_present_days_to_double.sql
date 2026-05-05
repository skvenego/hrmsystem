-- Change present_days column from INTEGER to DOUBLE to support decimal values (e.g., 0.5 for half-day present)
ALTER TABLE payslips MODIFY COLUMN present_days DOUBLE;
