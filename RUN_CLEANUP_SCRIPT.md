# How to Run Database Cleanup Script

## Problem Being Fixed:
1. ✅ Duplicate payroll records for same employee+month
2. ✅ Payslips showing old/dummy employee data

## What This Script Does:
1. Shows current payroll records (so you can see duplicates)
2. Deletes duplicate payroll records (keeps oldest one)
3. Adds UNIQUE constraint to prevent future duplicates
4. Verifies cleanup was successful

---

## Method 1: Using MySQL Workbench (Recommended)

### Step 1: Open MySQL Workbench
- Launch MySQL Workbench
- Connect to your local MySQL server (usually localhost:3306)
- Username: `root`
- Password: (your MySQL password)

### Step 2: Open the SQL Script
- Click **File** → **Open SQL Script**
- Navigate to: `C:\Users\GCV\Projects\hrmsystem\cleanup_database.sql`
- Click **Open**

### Step 3: Execute the Script
- You'll see the SQL code in the editor
- Click the **Lightning Bolt** icon (⚡) or press **Ctrl+Shift+Enter**
- Watch the output in the results panel below

### Step 4: Review Results
You should see several result sets:
1. **Current Employees** - List of all employees
2. **Current Payroll Records** - Shows duplicates if any exist
3. **Check for Duplicates** - Lists any duplicate records
4. **Deleting Duplicates** - Confirmation message
5. **Duplicates After Cleanup** - Should show "0 rows" (success!)
6. **Adding Unique Constraint** - Confirmation
7. **Final Payroll Records** - Clean data with no duplicates

---

## Method 2: Using MySQL Command Line

### Step 1: Open Command Prompt
Press `Win + R`, type `cmd`, press Enter

### Step 2: Navigate to Project
```cmd
cd C:\Users\GCV\Projects\hrmsystem
```

### Step 3: Run the Script
```cmd
mysql -u root -p hrmsystem < cleanup_database.sql
```

Enter your MySQL password when prompted.

---

## Method 3: Manual Execution (If above methods fail)

### Open MySQL Command Line Client:
1. Click Windows Start
2. Search for "MySQL Command Line Client"
3. Click to open
4. Enter password when prompted

### Copy and Paste Each Section:

```sql
USE hrmsystem;
```

```sql
-- See current employees
SELECT id, first_name, last_name, email FROM employee ORDER BY id;
```

```sql
-- See current payroll with duplicates
SELECT p.id, p.employee_id, e.first_name, e.last_name, p.month, p.year 
FROM payroll p JOIN employee e ON p.employee_id = e.id 
ORDER BY p.employee_id, p.year, p.month;
```

```sql
-- Find duplicates
SELECT employee_id, month, year, COUNT(*) as count
FROM payroll GROUP BY employee_id, month, year HAVING count > 1;
```

```sql
-- Delete duplicates
DELETE p1 FROM payroll p1
INNER JOIN payroll p2 
WHERE p1.id > p2.id 
  AND p1.employee_id = p2.employee_id 
  AND p1.month = p2.month 
  AND p1.year = p2.year;
```

```sql
-- Add unique constraint
ALTER TABLE payroll ADD UNIQUE KEY unique_employee_month_year (employee_id, month, year);
```

```sql
-- Verify no duplicates remain
SELECT employee_id, month, year, COUNT(*) as count
FROM payroll GROUP BY employee_id, month, year ORDER BY employee_id;
```

---

## Expected Results:

### Before Cleanup:
```
Employee ID | Name          | Month | Year | Count
------------|---------------|-------|------|------
3           | Sachin Verma  |   3   | 2026 |   3    ← 3 duplicates!
5           | Ram Murat     |   3   | 2026 |   2    ← 2 duplicates!
```

### After Cleanup:
```
Employee ID | Name          | Month | Year | Count
------------|---------------|-------|------|------
3           | Sachin Verma  |   3   | 2026 |   1    ✓
5           | Ram Murat     |   3   | 2026 |   1    ✓
```

---

## After Running the Script:

### 1. Refresh Your Browser
Press `Ctrl + F5` to clear cache and reload

### 2. Go to Payroll Page
- URL: `http://localhost:8080/#/dashboard/payroll`
- You should now see only ONE record per employee per month

### 3. Generate Fresh Payroll (if needed)
- If payroll was deleted, click "Generate Payroll"
- Select Month: 3, Year: 2026
- Click "Generate" **ONCE** (don't double-click!)

### 4. Check Payslips Page
- Go to Payslips page
- Search for employees by name
- You should see REAL names (Sachin Verma, Ram Murat, etc.)
- NOT dummy names like "John Smith"

---

## Troubleshooting:

### Error: "Cannot delete from target table"
This is a MySQL limitation. Use this workaround:

```sql
-- Create temporary table with duplicates
CREATE TEMPORARY TABLE temp_duplicates AS
SELECT employee_id, month, year, MAX(id) as max_id
FROM payroll
GROUP BY employee_id, month, year
HAVING COUNT(*) > 1;

-- Delete keeping only the latest
DELETE FROM payroll 
WHERE id IN (
    SELECT d.id FROM (
        SELECT p1.id 
        FROM payroll p1
        INNER JOIN payroll p2 
        WHERE p1.id > p2.id 
          AND p1.employee_id = p2.employee_id 
          AND p1.month = p2.month 
          AND p1.year = p2.year
    ) as d
);

DROP TEMPORARY TABLE temp_duplicates;
```

### Error: "Duplicate key name"
The unique constraint already exists. That's fine! Skip to verification.

### Error: "Table doesn't exist"
Make sure you're connected to the correct database (`hrmsystem`).

---

## Success Indicators:

✅ Script runs without errors
✅ "Duplicates After Cleanup" shows 0 rows
✅ "Unique constraint added" message appears
✅ Final list shows each employee only once per month
✅ Browser shows clean data after refresh

---

**Once complete, your payroll will have NO duplicates and payslips will show REAL employee data!** 🎉
