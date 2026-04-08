# 🔧 Quick Fix - Profile Shows "-" for Phone & Address

## ⚡ ONE-LINE FIX

**Copy and paste this into MySQL Workbench or your MySQL client:**

```sql
USE hrmsystem; ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20); ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT; UPDATE users SET phone=COALESCE(phone,'+0000000000'), address=COALESCE(address,'Not provided') WHERE phone IS NULL OR address IS NULL; SELECT id, username, email, phone, address FROM users ORDER BY id;
```

That's it! Run that command and your Profile/Settings pages will show data immediately! ✅

---

## 📋 Step-by-Step Instructions

### Option 1: MySQL Workbench (Recommended)

1. Open **MySQL Workbench**
2. Connect to your database
3. Click on `hrmsystem` database
4. Paste this SQL:
   ```sql
   USE hrmsystem;
   
   ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
   ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT;
   
   UPDATE users 
   SET phone = COALESCE(phone, '+0000000000'), 
       address = COALESCE(address, 'Not provided') 
   WHERE phone IS NULL OR address IS NULL;
   
   SELECT id, username, email, phone, address FROM users ORDER BY id;
   ```
5. Click **Execute** (lightning bolt icon)
6. See results showing all users with phone/address!

### Option 2: Command Line

If you know MySQL root password:

```bash
mysql -u root -p hrmsystem < quick_fix_profile.sql
```

### Option 3: Heidisql / phpMyAdmin

1. Open query tab
2. Paste the SQL from Option 1
3. Execute

---

## ✅ What This Does

1. **Adds missing columns** (if they don't exist yet)
2. **Updates ALL users** with NULL phone/address to have default values
3. **Shows you the results** so you can verify

---

## 🎯 After Running

1. **Refresh your browser** (Ctrl+F5)
2. Go to **Profile page**
3. You should now see:
   ```
   Phone Number: +0000000000
   Address: Not provided
   ```
4. Click **"Edit"** button
5. Change to your REAL phone and address
6. Click **"Save"**
7. Done! Data is now permanent! ✅

---

## 🧪 Verify It Worked

### Before Fix:
```
Phone Number: -
Address: -
```

### After Fix:
```
Phone Number: +0000000000  ← Has value now!
Address: Not provided      ← Has value now!
```

---

## ❓ Why Did This Happen?

**Simple explanation:**

Your user account was created **before** we added phone/address fields to the database. So your user record has `NULL` (empty) values for those fields.

This fix:
1. Adds the columns if missing
2. Fills in default values for old users
3. Makes Profile/Settings pages display data

---

## 🔍 Check Your Database

To see if columns already exist:

```sql
DESCRIBE users;
```

Look for these columns:
- ✅ `phone` - should be there
- ✅ `address` - should be there

If missing, the ALTER TABLE commands above will add them!

---

## 📞 Still Shows "-" After Fix?

Try these:

### 1. Clear Browser Cache
- Press **Ctrl+Shift+Delete**
- Clear cached images
- Refresh page (**Ctrl+F5**)

### 2. Logout and Login Again
- Logout from application
- Login again
- Check Profile page

### 3. Check User ID
Make sure you're updating the right user:

```sql
-- Find your user
SELECT * FROM users WHERE username = 'your_username';

-- Update specific user by ID
UPDATE users SET phone='+1234567890', address='My Address' WHERE id=1;
```

---

## ✨ Expected Results

After running the SQL:

**All users will have:**
- Phone: Either their real phone OR `+0000000000`
- Address: Either their real address OR `Not provided`

**Then you can:**
1. Go to Profile/Settings
2. Click Edit
3. Replace with YOUR real data
4. Save
5. Data persists permanently! ✅

---

**Bottom line:** Run the SQL command at the top, then refresh your Profile page. Problem solved! 🎉
