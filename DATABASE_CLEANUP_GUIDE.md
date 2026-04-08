# Database Cleanup Guide - Remove Unknown/Invalid Data 🧹

## ⚠️ IMPORTANT WARNING

This cleanup will **DELETE** data from your database. Make sure you understand what will be removed before proceeding!

---

## What Will Be Deleted?

### 1. **Invalid Payslips** ❌
- Payslips with NULL employee_id (shows as "Unknown")
- Payslips with ZERO basic salary (₹0)
- Payslips with ZERO net salary (₹0)
- Duplicate payslips for same employee+month

### 2. **Invalid Payroll Records** ❌
- Payroll with NULL employee_id
- Payroll with ZERO net salary
- Duplicate payroll for same employee+month+year

### 3. **Orphaned Attendance** ❌
- Attendance records with NULL employee_id
- Attendance with NULL date
- Future attendance (beyond today)

### 4. **Invalid Leave Records** ❌
- Leave with NULL employee_id
- Leave with NULL start_date
- Old cancelled leave (older than 6 months)

---

## 🚀 How to Run Cleanup (Choose ONE method)

### Method 1: Using PowerShell Script (Recommended)

1. **Open PowerShell** in your project folder:
   ```powershell
   cd c:\Users\GCV\Projects\hrmsystem
   ```

2. **Run the cleanup script**:
   ```powershell
   .\run_cleanup.ps1
   ```

3. **Type "yes"** when prompted to confirm

4. **Wait for completion** message

---

### Method 2: Manual SQL (Advanced)

1. **Open MySQL Command Line** or **MySQL Workbench**

2. **Connect to database**:
   ```sql
   USE hrm_db;
   ```

3. **Run the SQL script**:
   - Copy content from `cleanup_database_invalid_data.sql`
   - Paste and execute in MySQL

---

## ✅ After Cleanup

### Step 1: Verify Results

Refresh your browser and check:

1. **Payslips Page** (`/dashboard/payslips`)
   - Should show employee names (NOT "Unknown")
   - Should show salaries > ₹0
   - No duplicate entries

2. **Payroll Page** (`/dashboard/payroll`)
   - Should show all active employees
   - Should have valid salary data

### Step 2: Clear Browser Cache

Press `Ctrl + Shift + R` to hard refresh

### Step 3: Regenerate Data (if needed)

If you deleted too much:

1. Go to **Payroll Page**
2. Click **"Generate Payroll"**
3. Select current month
4. Generate for all employees
5. Then refresh **Payslips Page**

---

## 📊 What You'll See After Cleanup

### Before Cleanup:
```
❌ Unknown          - Basic: ₹0, Net: ₹0
❌ Unknown          - Basic: ₹0, Net: ₹0  
❌ John Doe         - Basic: ₹45,000, Net: ₹38,500
❌ Unknown          - Basic: ₹0, Net: ₹0
```

### After Cleanup:
```
✅ John Doe         - Basic: ₹45,000, Net: ₹38,500
✅ Jane Smith       - Basic: ₹52,000, Net: ₹44,200
✅ Raj Kumar        - Basic: ₹38,000, Net: ₹32,100
```

---

## 🛡️ Safety Precautions

### Optional: Create Backup First

Add these lines at the **beginning** of `cleanup_database_invalid_data.sql`:

```sql
-- Create backups (optional)
CREATE TABLE payslips_backup AS SELECT * FROM payslips;
CREATE TABLE payroll_backup AS SELECT * FROM payroll;
CREATE TABLE attendance_backup AS SELECT * FROM attendance;
CREATE TABLE leave_backup AS SELECT * FROM leave_table;
```

This creates backup tables before deletion. You can restore later if needed.

---

## 🔍 Check Current Data Status

Before cleanup, you can check how many invalid records exist:

```sql
-- Count invalid payslips
SELECT COUNT(*) as invalid_payslips 
FROM payslips 
WHERE employee_id IS NULL OR basic_salary = 0 OR net_salary = 0;

-- Count invalid payroll
SELECT COUNT(*) as invalid_payroll 
FROM payroll 
WHERE employee_id IS NULL OR net_salary = 0;

-- Count orphaned attendance
SELECT COUNT(*) as orphaned_attendance 
FROM attendance 
WHERE employee_id IS NULL;
```

---

## ❓ Troubleshooting

### Error: "mysql is not recognized"

**Solution:** Add MySQL to PATH or use full path:

```powershell
# Example with full MySQL path
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p hrm_db < cleanup_database_invalid_data.sql
```

### Error: "Access denied"

**Solution:** Update password in `run_cleanup.ps1`:

```powershell
$dbPassword = "your_password_here"  # Line 13
```

### Error: "Database not found"

**Solution:** Verify database name in files:
- Change `hrm_db` to `hrmsystem` if that's your database name
- Update in both `.sql` and `.ps1` files

---

## 📝 Summary

**Files Created:**
1. ✅ `cleanup_database_invalid_data.sql` - SQL commands
2. ✅ `run_cleanup.ps1` - PowerShell automation
3. ✅ This guide document

**What Gets Cleaned:**
- Invalid payslips (NULL employee, ZERO salary)
- Duplicate records
- Orphaned data
- Old cancelled leaves

**Result:**
- Clean database with only valid data
- Payslips show proper employee names
- Salaries display correctly (no more ₹0)
- Better performance (optimized tables)

---

## 🎯 Quick Start

**Ready to clean?** Just run:

```powershell
cd c:\Users\GCV\Projects\hrmsystem
.\run_cleanup.ps1
```

Then type **"yes"** and wait for success message!

---

**Last Updated:** March 26, 2026  
**Status:** ✅ Ready to Use  
**Risk Level:** Medium (deletes data)  
**Recommendation:** Create backup first if unsure
