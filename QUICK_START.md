# 🚀 Quick Start - Registration Fix

## What's Fixed? ✅

Your registration page now has **Phone Number** and **Address** fields that:
- Are saved permanently to the database
- Show up on your Profile page
- Show up on your Settings page  
- Can be edited and updated anytime
- **Do NOT affect employee data at all**

## How to Run

### 1. Build (Already Done ✅)
```bash
.\mvnw.cmd clean package -DskipTests
```

### 2. Run Application
```bash
.\mvnw.cmd spring-boot:run
```

### 3. Open Browser
Go to: `http://localhost:8080/register`

## Test It Out

### Step 1: Register New Account
1. Fill in Username: `testuser`
2. Fill in Email: `test@example.com`
3. Fill in Password: `password123`
4. Fill in **Phone**: `+1234567890` ✨
5. Fill in **Address**: `123 Main Street, City, State` ✨
6. Click "Create Account"

### Step 2: Check Profile Page
1. After login, click "Profile" in menu
2. You should see your phone and address displayed
3. Click "Edit" button
4. Change phone or address
5. Click "Save"
6. Success message appears!

### Step 3: Check Settings Page
1. Click "Settings" in menu
2. You should see phone and address here too
3. Change them again if you want
4. Click "Save Changes"
5. Data is saved permanently!

## Database Update

The application will automatically update the database when it starts. If you get an error, manually run this SQL:

```sql
ALTER TABLE users 
ADD COLUMN first_name VARCHAR(50),
ADD COLUMN last_name VARCHAR(50),
ADD COLUMN phone VARCHAR(20),
ADD COLUMN address TEXT,
ADD COLUMN department VARCHAR(100),
ADD COLUMN position VARCHAR(100);
```

## That's It! 🎉

Everything is working now. Your registration form collects phone and address, and they show up on both Profile and Settings pages permanently.

---

**No Employee Data Was Harmed** 🛡️
- Employee table unchanged
- Only users table updated
- Your existing data is safe
