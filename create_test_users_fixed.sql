-- Create test users for dual approval system
USE hrm_db;

-- Password: password123 (BCrypt hashed)
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- Create test employee user (sachin)
INSERT INTO users (username, email, password, role, is_active)
VALUES ('sachin', 'sachin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_EMPLOYEE', TRUE);

-- Create test accountant user
INSERT INTO users (username, email, password, role, is_active)
VALUES ('accountant', 'accountant@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_ACCOUNTANT', TRUE);

-- Create test director user  
INSERT INTO users (username, email, password, role, is_active)
VALUES ('director', 'director@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_DIRECTOR', TRUE);

-- Create HR user
INSERT INTO users (username, email, password, role, is_active)
VALUES ('hr', 'hr@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ROLE_HR', TRUE);

-- Verify users created
SELECT id, username, email, role, is_active FROM users WHERE username IN ('sachin', 'accountant', 'director', 'hr');
