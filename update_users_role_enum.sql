-- Update users table role enum to support dual approval system
USE hrm_db;

-- Step 1: Expand the ENUM first to accept all values (including old and new)
ALTER TABLE users 
MODIFY COLUMN role ENUM('ADMIN', 'EMPLOYEE', 'HR', 'MANAGER', 'ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_HR', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DIRECTOR') NOT NULL;

-- Step 2: Now update existing users to use ROLE_ prefix
UPDATE users SET role = 'ROLE_EMPLOYEE' WHERE role = 'EMPLOYEE';

-- Step 3: Remove old ENUM values by setting only the ROLE_ ones
ALTER TABLE users 
MODIFY COLUMN role ENUM('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_HR', 'ROLE_MANAGER', 'ROLE_ACCOUNTANT', 'ROLE_DIRECTOR') NOT NULL;

-- Verify the change
SHOW COLUMNS FROM users LIKE 'role';

-- Show existing roles
SELECT DISTINCT role FROM users;

-- Now create test users
INSERT INTO users (username, email, password, role, is_active)
VALUES ('sachin', 'sachin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_EMPLOYEE', TRUE);

INSERT INTO users (username, email, password, role, is_active)
VALUES ('accountant', 'accountant@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ACCOUNTANT', TRUE);

INSERT INTO users (username, email, password, role, is_active)
VALUES ('director', 'director@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_DIRECTOR', TRUE);

INSERT INTO users (username, email, password, role, is_active)
VALUES ('hr', 'hr@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_HR', TRUE);

-- Verify users created
SELECT id, username, email, role, is_active FROM users WHERE username IN ('sachin', 'accountant', 'director', 'hr');
