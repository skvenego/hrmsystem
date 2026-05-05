-- Change absent_days column from INTEGER to DOUBLE to support decimal values (e.g., 0.5 for half-day absent)
ALTER TABLE payslips MODIFY COLUMN absent_days DOUBLE;
