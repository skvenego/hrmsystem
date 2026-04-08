# 📝 STEP-BY-STEP DATABASE MIGRATION INSTRUCTIONS

## 🎯 EASIEST METHOD - Using MySQL Workbench

### **Step 1: Open MySQL Workbench**
Double-click the MySQL Workbench icon on your desktop

---

### **Step 2: Connect to Local Server**
Click on **"Local instance MySQL80"** (or similar) connection
- Enter password if prompted
- You should see the database schema on the left

---

### **Step 3: Select hrmsystem Database**
In the **SCHEMAS** panel (left side):
- Find `hrmsystem`
- Double-click it (it will become bold = active)

---

### **Step 4: Open SQL Script File**
From the menu:
1. Click **File** → **Open SQL Script**
2. Navigate to: `C:\Users\GCV\Projects\hrmsystem\`
3. Select file: `database_migration_dual_approval.sql`
4. Click **Open**

---

### **Step 5: Execute the Script**
You'll see the SQL code in a new tab:
1. Click the **⚡** lightning bolt icon in the toolbar
   OR press **Ctrl+Shift+Enter**
2. Wait for execution (should take 2-5 seconds)

---

### **Step 6: Check Results**
Look at the **Output** panel at the bottom:
- ✅ Should show: `✅ Dual Approval Payroll Migration Complete!`
- ✅ Should show: `MIGRATION COMPLETED SUCCESSFULLY!`
- ❌ If you see errors, they'll be in red

---

### **Step 7: Verify Tables Updated**

#### Check payroll table:
```sql
DESCRIBE payroll;
```
You should see these NEW columns:
- `requires_dual_approval`
- `accountant_approved`
- `director_approved`
- `final_approval_date`
- `payslip_generated`

#### Check new table exists:
```sql
SHOW TABLES LIKE 'payroll_approvals';
```
Should return: `payroll_approvals`

---

## 🔄 ALTERNATIVE METHOD - Using PowerShell

If MySQL Workbench is not available:

### **Step 1: Open PowerShell as Administrator**
Right-click PowerShell → Run as Administrator

### **Step 2: Navigate to Project**
```powershell
cd C:\Users\GCV\Projects\hrmsystem
```

### **Step 3: Run Migration Script**
```powershell
.\run_database_migration.ps1
```

### **Step 4: Follow Prompts**
- Enter MySQL path (or press Enter for default)
- Enter MySQL root password
- Script will run automatically

---

## ✅ WHAT YOU SHOULD SEE AFTER MIGRATION

### **Database Changes:**

**Before Migration:**
```
payroll table:
- id
- employee_id
- month
- year
- basic_salary
- ... (old columns)
- status
```

**After Migration:**
```
payroll table:
- id
- employee_id
- month
- year
- basic_salary
- ... (old columns)
- status
- requires_dual_approval     ← NEW
- accountant_approved        ← NEW
- director_approved          ← NEW
- final_approval_date        ← NEW
- payslip_generated          ← NEW

NEW TABLE:
- payroll_approvals          ← NEW TABLE
```

---

## 🎯 VERIFICATION QUERY

Run this query to see everything working:

```sql
SELECT 
    p.id AS payroll_id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee,
    p.month,
    p.year,
    p.status,
    p.accountant_approved AS acct_approved,
    p.director_approved AS dir_approved,
    p.payslip_generated AS ready,
    CASE 
        WHEN p.accountant_approved = 1 AND p.director_approved = 1 
        THEN '✅ Ready'
        ELSE '⏳ Pending'
    END AS status_display
FROM payroll p
JOIN employees e ON p.employee_id = e.id
ORDER BY p.year DESC, p.month DESC
LIMIT 5;
```

**Expected Result:**
```
+------------+---------------+-------+------+------------------+--------------+-------------+--------+--------------+
| payroll_id | employee      | month | year | status           | acct_approved| dir_approved| ready  | status_display|
+------------+---------------+-------+------+------------------+--------------+-------------+--------+--------------+
|     15     | Sachin Kumar  |   3   | 2026 | PENDING_APPROVAL |            0 |           0 |      0 | ⏳ Pending    |
|     14     | Ram Murat     |   3   | 2026 | PAID             |            1 |           1 |      1 | ✅ Ready      |
+------------+---------------+-------+------+------------------+--------------+-------------+--------+--------------+
```

---

## ⚠️ COMMON ISSUES & SOLUTIONS

### **Issue 1: "Table doesn't exist"**
**Solution:** Make sure you selected `hrmsystem` database first

### **Issue 2: "Column already exists"**
**Solution:** Migration already ran - this is OK! Skip to verification

### **Issue 3: "Access denied"**
**Solution:** Wrong password - re-enter correct root password

### **Issue 4: Nothing happens**
**Solution:** Check Output panel - scroll up to see messages

---

## 🚀 AFTER MIGRATION COMPLETE

### **Next Step: Restart Your Application**

1. **Stop current application:**
   - Go to terminal where Spring Boot is running
   - Press `Ctrl+C`

2. **Restart:**
   ```bash
   mvn spring-boot:run
   ```

3. **Wait for:**
   ```
   Started HrmsystemApplication in X.XXX seconds
   ```

4. **Test the API:**
   Open browser and go to:
   ```
   http://localhost:8080/api/payroll/approvals/pending/accountant
   ```
   
   Should return: `[]` (empty array) or list of pending approvals

---

## 📊 SUMMARY OF CHANGES

### **Files Created by You:**
1. ✅ `PayrollApproval.java` - Model
2. ✅ `PayrollApprovalRepository.java` - Repository
3. ✅ `PayrollApprovalService.java` - Service
4. ✅ `PayrollApprovalController.java` - Controller
5. ✅ `database_migration_dual_approval.sql` - Migration script
6. ✅ `run_database_migration.ps1` - PowerShell helper

### **What Migration Does:**
1. ✅ Adds 5 columns to `payroll` table
2. ✅ Creates 1 new table `payroll_approvals`
3. ✅ Sets default values for existing data
4. ✅ Creates foreign key relationships
5. ✅ Adds indexes for performance

### **Total Database Changes:**
```
Tables Modified: 1 (payroll)
Tables Created: 1 (payroll_approvals)
Columns Added: 5
Foreign Keys: 2
Indexes: 3
```

---

## 🎉 SUCCESS CHECKLIST

After migration, verify:

- [ ] MySQL Workbench shows no errors
- [ ] Output says "Migration Complete"
- [ ] `DESCRIBE payroll` shows 5 new columns
- [ ] `SHOW TABLES` includes `payroll_approvals`
- [ ] Spring Boot application starts successfully
- [ ] API endpoint `/api/payroll/approvals/pending/accountant` works
- [ ] No database errors in application logs

---

## 📞 QUICK HELP COMMANDS

### Check if migration ran:
```sql
SELECT COUNT(*) FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'hrmsystem' 
AND TABLE_NAME = 'payroll'
AND COLUMN_NAME IN ('accountant_approved', 'director_approved');
-- Should return: 2
```

### View new table structure:
```sql
DESCRIBE payroll_approvals;
```

### See all approval records:
```sql
SELECT * FROM payroll_approvals LIMIT 10;
```

---

**Ready to migrate!** Follow Steps 1-7 above using MySQL Workbench (easiest method). 🚀
