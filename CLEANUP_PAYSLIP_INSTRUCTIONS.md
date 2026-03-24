# 🧹 CLEANUP PAYSLIP DATA - INSTRUCTIONS

## Problem
Your payslips are showing "Unknown" or incorrect data. This is because there are dummy/incorrect records in the database.

## ✅ Solution - 2 Options

### **Option 1: Using MySQL Workbench (RECOMMENDED)**

1. **Open MySQL Workbench**

2. **Connect to your database** (usually `root@localhost`)

3. **Run these SQL commands:**

```sql
USE hrmsystem;

-- View current payslips (optional)
SELECT * FROM payslip ORDER BY created_date DESC;

-- Delete all payslips
DELETE FROM payslip;

-- Verify deletion
SELECT COUNT(*) as remaining_payslips FROM payslip;
-- Should return 0

COMMIT;
```

4. **Refresh your browser** (Ctrl + F5)

5. **Go to Payroll page** → Click "Generate Payroll" → Select current month/year → Generate

---

### **Option 2: Using Command Line**

If you know your MySQL root password, run:

```powershell
# If password is 'root':
Get-Content cleanup_payslips.sql | mysql -u root -proot hrmsystem

# If password is empty (XAMPP default):
Get-Content cleanup_payslips.sql | mysql -u root hrmsystem

# If password is something else:
Get-Content cleanup_payslips.sql | mysql -u root -pYOUR_PASSWORD hrmsystem
```

---

## 🔍 What This Does

1. ✅ Deletes all existing payslips with unknown/incorrect data
2. ✅ Keeps employee data intact
3. ✅ Allows fresh payroll generation with correct calculations

---

## 📊 After Cleanup

When you regenerate payroll, it will calculate correctly based on:

- ✅ Employee's actual salary from the `employee` table
- ✅ Attendance records (present/absent days)
- ✅ Proper allowances (HRA, DA, TA)
- ✅ Correct deductions (PF, Tax, Insurance)
- ✅ Per-day salary calculation for absent days

---

## 🎯 Expected Result

After regenerating:

| Employee | Basic Salary | Gross Salary | Deductions | Net Salary |
|----------|-------------|--------------|------------|------------|
| John Doe | ₹50,000     | ₹67,500      | ₹8,100     | ₹59,400    |

**Calculation:**
- Basic: ₹50,000
- HRA (20%): ₹10,000
- DA (10%): ₹5,000  
- TA (5%): ₹2,500
- **Gross**: ₹67,500
- PF (12%): ₹6,000
- Tax: ₹1,500
- Insurance: ₹600
- **Total Deductions**: ₹8,100
- **Net**: ₹59,400

---

## ❓ Need Help?

If you see errors:
1. Check if MySQL is running
2. Verify database name is `hrmsystem`
3. Make sure you're using correct root password
4. Check Application.yml for actual database credentials
