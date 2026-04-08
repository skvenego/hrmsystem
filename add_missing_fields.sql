-- Fix employee table structure to match form exactly
ALTER TABLE employees 
ADD COLUMN pf DECIMAL(12,2) DEFAULT 0 AFTER other_allowance,
ADD COLUMN tax DECIMAL(12,2) DEFAULT 0 AFTER pf;

-- Remove incorrect user_id column
ALTER TABLE employees DROP COLUMN user_id;

-- Show updated table structure
DESCRIBE employees;

-- Verify the new fields were added
SELECT 'Table structure updated successfully!' as status;
