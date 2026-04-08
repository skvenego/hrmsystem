-- ============================================
-- Verify Notification System Setup
-- ============================================

-- This script verifies that everything is set up correctly 
-- for email notifications to work

-- Step 1: Check if employee exists with your email
SELECT 'STEP 1: Employee Check' as step;
SELECT id, CONCAT(first_name, ' ', last_name) as name, email, phone, designation
FROM employees
WHERE email = 'skvenego@gmail.com';

-- Expected: Should show at least 1 row (Sachin Kumar's employee record)


-- Step 2: Check if user account exists with same email
SELECT 'STEP 2: User Account Check' as step;
SELECT id, username, email, role, created_at
FROM users
WHERE email = 'skvenego@gmail.com';

-- Expected: Should show at least 1 row (your login account)


-- Step 3: Check notification preferences
SELECT 'STEP 3: Notification Preferences Check' as step;
SELECT 
    u.username,
    u.email,
    np.id as pref_id,
    np.email_notifications,
    np.push_notifications,
    np.sms_notifications,
    np.leave_updates,
    np.payroll_updates,
    np.attendance_updates,
    np.created_at
FROM notification_preferences np
JOIN users u ON np.user_id = u.id
WHERE u.email = 'skvenego@gmail.com';

-- Expected: Should show 1 row with all preferences
-- If NO rows found, run the fix below:

/*
-- FIX: Create default preferences if missing
INSERT INTO notification_preferences (user_id, email_notifications, push_notifications, sms_notifications, leave_updates, payroll_updates, attendance_updates)
SELECT id, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE
FROM users
WHERE email = 'skvenego@gmail.com'
AND id NOT IN (SELECT user_id FROM notification_preferences WHERE user_id IS NOT NULL);
*/


-- Step 4: Show all employees and their corresponding users
SELECT 'STEP 4: All Employees & Users Mapping' as step;
SELECT 
    e.id as employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    e.email as employee_email,
    u.id as user_id,
    u.username,
    CASE 
        WHEN u.id IS NOT NULL THEN '✅ Has User Account'
        ELSE '❌ No User Account'
    END as status
FROM employees e
LEFT JOIN users u ON e.email = u.email
ORDER BY e.id;


-- Step 5: Summary
SELECT 'STEP 5: Summary' as step;
SELECT 
    'Total Employees' as metric, 
    COUNT(*) as count 
FROM employees
UNION ALL
SELECT 'Total Users', COUNT(*) FROM users
UNION ALL
SELECT 'Users with Preferences', COUNT(*) FROM notification_preferences
UNION ALL
SELECT 'Employees with Matching Users', COUNT(*) 
FROM employees e
JOIN users u ON e.email = u.email;
