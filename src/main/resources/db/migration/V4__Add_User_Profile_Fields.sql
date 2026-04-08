-- Add additional fields to users table for profile management

ALTER TABLE users 
ADD COLUMN first_name VARCHAR(50) AFTER email,
ADD COLUMN last_name VARCHAR(50) AFTER first_name,
ADD COLUMN phone VARCHAR(20) AFTER last_name,
ADD COLUMN address TEXT AFTER phone,
ADD COLUMN department VARCHAR(100) AFTER address,
ADD COLUMN position VARCHAR(100) AFTER department;
