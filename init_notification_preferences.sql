-- ============================================
-- Initialize Notification Preferences
-- ============================================

-- This script ensures ALL existing users have notification preferences set up

-- Step 1: Insert default preferences for users who don't have them
INSERT INTO notification_preferences (user_id, email_notifications, push_notifications, sms_notifications, leave_updates, payroll_updates, attendance_updates)
SELECT 
    id as user_id,
    TRUE as email_notifications,
    TRUE as push_notifications,
    FALSE as sms_notifications,
    TRUE as leave_updates,
    TRUE as payroll_updates,
    TRUE as attendance_updates
FROM users
WHERE id NOT IN (
    SELECT user_id FROM notification_preferences WHERE user_id IS NOT NULL
);

-- Step 2: Verify all users have preferences
SELECT 
    'Setup Complete' as status,
    COUNT(DISTINCT u.id) as total_users,
    COUNT(DISTINCT np.user_id) as users_with_preferences,
    COUNT(DISTINCT u.id) - COUNT(DISTINCT np.user_id) as users_missing_preferences
FROM users u
LEFT JOIN notification_preferences np ON u.id = np.user_id;

-- Step 3: Show all preferences
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    np.email_notifications,
    np.push_notifications,
    np.sms_notifications,
    np.leave_updates,
    np.payroll_updates,
    np.attendance_updates,
    np.created_at,
    np.updated_at
FROM users u
LEFT JOIN notification_preferences np ON u.id = np.user_id
ORDER BY u.id;
