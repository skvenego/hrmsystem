-- ============================================
-- VERIFY YOUR PROFILE & SETTINGS DATA
-- ============================================

-- This script shows what's currently in your users table
SELECT 
    id,
    username as 'Name',
    email as 'Email',
    CASE 
        WHEN phone IS NULL THEN '❌ NULL'
        WHEN phone = '' THEN '❌ Empty'
        ELSE '✅ ' + phone 
    END as 'Phone Status',
    CASE 
        WHEN address IS NULL THEN '❌ NULL'
        WHEN address = '' THEN '❌ Empty'
        ELSE '✅ ' + address 
    END as 'Address Status'
FROM users 
WHERE username = 'pradeep';

-- ============================================
-- EXPECTED OUTPUT AFTER FIX:
-- ============================================
-- id | Name    | Email                | Phone Status      | Address Status
----|---------|----------------------|-------------------|---------------------------
-- 13 | pradeep | pradeep123@gmail.com | ✅ 8726169192    | ✅ Enego Service Private Limited
-- ============================================

-- If you see ❌ marks, run this to fix:
UPDATE users 
SET 
    phone = COALESCE(NULLIF(phone, ''), '8726169192'),
    address = COALESCE(NULLIF(address, ''), 'Enego Service Private Limited')
WHERE username = 'pradeep';

-- Verify the fix worked
SELECT 
    id,
    username as 'Name',
    email as 'Email',
    phone as 'Phone',
    address as 'Address'
FROM users 
WHERE username = 'pradeep';
