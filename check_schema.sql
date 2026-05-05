-- Check actual table structure
DESCRIBE payslips;
DESCRIBE leave_balances;
DESCRIBE leaves;

-- Show all columns for payslips
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'payslips' AND TABLE_SCHEMA = DATABASE();

-- Show all columns for leave_balances
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'leave_balances' AND TABLE_SCHEMA = DATABASE();
