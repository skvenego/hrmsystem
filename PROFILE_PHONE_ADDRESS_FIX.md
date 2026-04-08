# ✅ Profile Phone & Address Display Fix - COMPLETE GUIDE

## 🐛 Problem You Reported

**You said:**
> ✔ name/email show  
> ❌ phone/address missing  
> why i want show corectly on my profile page here show only login page information not showing phone,address

---

## 🔍 Root Cause Found

### Issue 1: WRONG API Endpoint in Old Frontend

Your **old frontend** (`src/main/resources/static/hrm-frontend/`) was calling:
```javascript
// ❌ WRONG ENDPOINT - DOES NOT EXIST!
fetch(`/api/auth/users/${user.id}/profile`)
```

But your backend (`AuthController.java`) only has:
```java
@GetMapping("/users/{id}")      // ✅ Get user by ID
@PutMapping("/users/{id}")      // ✅ Update user profile
```

**There is NO `/api/auth/users/{id}/profile` endpoint!**

### Issue 2: Two Frontend Versions

You have **TWO** frontend directories:

1. **OLD:** `src/main/resources/static/hrm-frontend/` ← **BEING SERVED**
   - Has WRONG API endpoint
   - Calls `/api/auth/users/{id}/profile` (doesn't exist)
   - Returns 404 error
   - Phone/address don't load

2. **NEW:** `src/main/resources/hrmsystem-frontend/` ← **NOT BEING USED**
   - Has CORRECT API endpoint
   - Calls `/api/auth/users/{id}` (exists)
   - Returns data successfully
   - Would work correctly

### Issue 3: Database Data May Be Missing

Even with correct API, if `phone` or `address` are NULL/empty in database, they'll show as `-`.

---

## ✅ Fixes Applied

### Fix 1: Corrected API Endpoint in Old Frontend

**File:** `src/main/resources/static/hrm-frontend/src/pages/Profile.jsx`

**Changed Line 28:**
```javascript
// BEFORE (WRONG):
const res = await fetch(`/api/auth/users/${user.id}/profile`, {

// AFTER (CORRECT):
const res = await fetch(`/api/auth/users/${user.id}`, {
```

**Changed Line 66:**
```javascript
// BEFORE (WRONG):
const res = await fetch(`/api/auth/users/${user.id}/profile`, {

// AFTER (CORRECT):
const res = await fetch(`/api/auth/users/${user.id}`, {
```

---

## 🚀 How to Test the Fix

### Step 1: Rebuild the Frontend

Since you modified the source file, you need to rebuild:

```powershell
cd c:\Users\GCV\Projects\hrmsystem\src\main\resources\static\hrm-frontend
npm run build
```

This will regenerate the optimized JavaScript files in `src/main/resources/static/assets/`.

### Step 2: Restart Your Application

```powershell
# Stop the current Spring Boot application
# Then restart it
cd c:\Users\GCV\Projects\hrmsystem
mvn spring-boot:run
```

### Step 3: Clear Browser Cache

**IMPORTANT:** Your browser may have cached the old JavaScript files!

**Method 1: Hard Refresh**
- Press `Ctrl + Shift + R` (Windows/Linux)
- Or `Ctrl + F5`

**Method 2: Clear Cache Completely**
1. Open DevTools (F12)
2. Go to Network tab
3. Check "Disable cache" while DevTools is open
4. Refresh page (F5)

**Method 3: Incognito Mode**
- Open browser in incognito/private mode
- Login and test

### Step 4: Test Profile Page

1. **Login** to your account
2. **Navigate** to Profile page (`/dashboard/profile`)
3. **Check** if phone and address now display correctly

---

## 🧪 Verify Database Has Data

If phone/address still show `-`, check your database:

### Option 1: Using MySQL Workbench

Run this query:
```sql
SELECT id, username, email, phone, address 
FROM users 
WHERE username = 'pradeep';
```

### Option 2: Using the SQL Script

I created a script for you: `fix_profile_phone_address.sql`

Run it in MySQL:
```bash
mysql -u root -pSachin@123 hrmsystem < fix_profile_phone_address.sql
```

Expected output:
```
id | username | email                | phone      | address
---|----------|----------------------|------------|---------------------------
13 | pradeep  | pradeep123@gmail.com | 8726169192 | Enego Service Private Limited
```

If phone or address show as `NULL` or empty string, the script will update them.

---

## 📊 Expected Behavior After Fix

### Before Fix:
```
┌──────────────────────────────┐
│ Personal Information         │
├──────────────────────────────┤
│ pradeep                      │
│ EMPLOYEE                     │
├──────────────────────────────┤
│ Email Address                │
│ pradeep123@gmail.com         │ ✅ Shows
│                              │
│ Phone Number                 │
│ -                            │ ❌ Shows dash
│                              │
│ Address                      │
│ -                            │ ❌ Shows dash
└──────────────────────────────┘
```

### After Fix:
```
┌──────────────────────────────┐
│ Personal Information         │
├──────────────────────────────┤
│ pradeep                      │
│ EMPLOYEE                     │
├──────────────────────────────┤
│ Email Address                │
│ pradeep123@gmail.com         │ ✅ Shows
│                              │
│ Phone Number                 │
│ 8726169192                   │ ✅ Shows real data!
│                              │
│ Address                      │
│ Enego Service Private Limited│ ✅ Shows real data!
│                              │
│ [Edit Profile]               │
└──────────────────────────────┘
```

---

## 🔧 Debugging Steps

If it STILL doesn't work after the fix:

### 1. Check Browser Console

1. Open DevTools (F12)
2. Go to Console tab
3. Look for errors like:
   - `404 Not Found` → API endpoint wrong
   - `Failed to fetch` → Backend not running
   - `Cannot read property` → Data structure issue

### 2. Check Network Tab

1. Open DevTools (F12)
2. Go to Network tab
3. Refresh profile page
4. Find the request to `/api/auth/users/13`
5. Click on it
6. Check:
   - **Status:** Should be `200 OK`
   - **Response:** Should contain `phone` and `address` fields
   
Example response:
```json
{
  "id": 13,
  "username": "pradeep",
  "email": "pradeep123@gmail.com",
  "role": "EMPLOYEE",
  "isActive": true,
  "phone": "8726169192",
  "address": "Enego Service Private Limited"
}
```

### 3. Check Backend Logs

Look for any errors in Spring Boot console:
```
c.h.h.c.AuthController : Handling GET /api/auth/users/13
c.h.h.s.AuthService    : Getting user by ID: 13
```

---

## 📝 Summary

### What Was Fixed:
1. ✅ Changed API endpoint from `/api/auth/users/{id}/profile` → `/api/auth/users/{id}`
2. ✅ Fixed both GET (load) and PUT (save) requests
3. ✅ Created SQL script to verify database data

### What You Need to Do:
1. ✅ **Rebuild frontend** (`npm run build`)
2. ✅ **Restart backend** (`mvn spring-boot:run`)
3. ✅ **Clear browser cache** (`Ctrl+Shift+R`)
4. ✅ **Test profile page**
5. ✅ **Verify database** if still not showing

### Expected Result:
- ✅ Name shows correctly
- ✅ Email shows correctly
- ✅ **Phone shows correctly** (was broken, now fixed!)
- ✅ **Address shows correctly** (was broken, now fixed!)

---

## 🎯 Next Steps

1. **Rebuild the frontend** right now
2. **Restart your application**
3. **Clear browser cache** and refresh
4. **Check if phone/address appear**

If it still doesn't work:
- Run the SQL script to verify database
- Check browser console for errors
- Check network tab to see API response

---

## 📞 Support

If you're still having issues after following all steps:

1. Show me the browser console output (F12 → Console)
2. Show me the network tab response for `/api/auth/users/{id}`
3. Show me the result of running the SQL query

I can help debug further!
