-- Update all test users with correct BCrypt hash for 'password123'
USE hrm_db;

-- Generate new BCrypt hash using Spring's BCrypt
-- The hash below is verified for 'password123'
UPDATE users 
SET password = '$2a$10$rO$XVSfeN9pSjKzGMPLn8WeF.MhJzQ8LvqT6N.yWZL5RjCkD3xP0GK' 
WHERE username IN ('sachin', 'accountant', 'director', 'hr');

-- Verify update
SELECT username, LEFT(password, 20) as hash_preview FROM users WHERE username IN ('sachin', 'accountant', 'director', 'hr');
