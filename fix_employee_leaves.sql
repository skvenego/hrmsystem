-- Fix leaves assigned to wrong employee
-- Move leaves from Employee 16 to Employee 51 (Anubhav Sharma)

USE hrmsystem;

-- First, check current leave assignments
SELECT id, employee_id, start_date, end_date, status, paid_days, unpaid_days, total_days 
FROM leaves 
WHERE employee_id IN (16, 51);

-- Update leaves to assign them to Employee 51 (if they belong to Anubhav Sharma)
-- Uncomment the following after verifying:
-- UPDATE leaves SET employee_id = 51 WHERE employee_id = 16;

-- Verify the update
-- SELECT * FROM leaves WHERE employee_id = 51;
