-- ============================================
-- QUICK FIX: Check if pradeep has phone/address
-- ============================================

-- This will show you what's in the database RIGHT NOW
SELECT 
    id, 
    username, 
    email, 
    CASE 
        WHEN phone IS NULL THEN 'NULL'
        WHEN phone = '' THEN 'Empty String'
        ELSE phone 
    END as phone_status,
    CASE 
        WHEN address IS NULL THEN 'NULL'
        WHEN address = '' THEN 'Empty String'
        ELSE address 
    END as address_status
FROM users 
WHERE username = 'pradeep';

-- If phone or address are NULL/empty, run this:
UPDATE users 
SET 
    phone = '8726169192',
    address = 'Enego Service Private Limited'
WHERE username = 'pradeep' 
  AND (phone IS NULL OR phone = '' OR address IS NULL OR address = '');

-- Verify it worked
SELECT 
    id, 
    username, 
    email, 
    phone,
    address
FROM users 
WHERE username = 'pradeep';
