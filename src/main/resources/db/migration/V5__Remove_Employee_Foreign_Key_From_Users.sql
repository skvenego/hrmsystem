-- Remove employee dependency from users table - make it completely independent

-- Drop the foreign key constraint on employee_id
ALTER TABLE users DROP FOREIGN KEY users_ibfk_1;

-- Remove the employee_id column completely
ALTER TABLE users DROP COLUMN employee_id;
