-- Check if pradeep's data exists in users table
SELECT id, username, email, phone, address 
FROM users 
WHERE username = 'pradeep';

-- Expected result:
-- id=13, username=pradeep, email=pradeep123@gmail.com, phone=8726169192, address=Enego Service Private Limited

-- If phone or address is NULL, update them:
UPDATE users 
SET 
    phone = COALESCE(phone, '8726169192'),
    address = COALESCE(address, 'Enego Service Private Limited')
WHERE username = 'pradeep';

-- Verify the update worked
SELECT id, username, email, phone, address 
FROM users 
WHERE username = 'pradeep';
