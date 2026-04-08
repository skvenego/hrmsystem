-- Create test users for dual approval system
USE hrm_db;

-- Create test employee user
INSERT INTO users (username, email, password, role, is_active)
VALUES ('sachin', 'sachin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', TRUE);

-- Create test accountant user
INSERT INTO users (username, email, password, role, is_active)
VALUES ('accountant', 'accountant@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACCOUNTANT', TRUE);

-- Create test director user  
INSERT INTO users (username, email, password, role, is_active)
VALUES ('director', 'director@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DIRECTOR', TRUE);

-- Verify users created
SELECT id, username, email, role, is_active FROM users WHERE username IN ('sachin', 'accountant', 'director');
