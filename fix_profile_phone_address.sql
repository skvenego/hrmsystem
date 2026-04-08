-- ============================================
-- CHECK AND FIX PHONE/ADDRESS NOT SHOWING
-- ============================================

-- Step 1: Check what's actually in the users table
SELECT 
    id, 
    username, 
    email, 
    COALESCE(phone, 'NULL') as phone_status,
    COALESCE(address, 'NULL') as address_status
FROM users 
WHERE username = 'pradeep';

-- Step 2: If phone or address are NULL, update them
UPDATE users 
SET 
    phone = COALESCE(NULLIF(phone, ''), '8726169192'),
    address = COALESCE(NULLIF(address, ''), 'Enego Service Private Limited')
WHERE username = 'pradeep' 
  AND (phone IS NULL OR phone = '' OR address IS NULL OR address = '');

-- Step 3: Verify the update worked
SELECT 
    id, 
    username, 
    email, 
    phone,
    address
FROM users 
WHERE username = 'pradeep';

-- ============================================
-- EXPECTED RESULT AFTER FIX:
-- id | username | email                | phone      | address
----|----------|----------------------|------------|---------------------------
-- 13 | pradeep  | pradeep123@gmail.com | 8726169192 | Enego Service Private Limited
-- ============================================
