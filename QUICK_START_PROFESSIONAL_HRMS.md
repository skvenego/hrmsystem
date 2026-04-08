# 🚀 PROFESSIONAL HRMS - QUICK START GUIDE

## ⚡ Immediate Setup (5 Minutes)

### Step 1: Run Database Migration

Open PowerShell and run:

```powershell
cd c:\Users\GCV\Projects\hrmsystem
mysql -u root -pSachin@123 hrm_db < database_migration.sql
```

**Expected Output:**
```
✅ Migration Completed Successfully!
PROBATION PERIODS: 13 records created
LEAVE BALANCES: 13 records created
EMPLOYEES IN PROBATION: X records
CONFIRMED EMPLOYEES: Y records
```

---

### Step 2: Verify New Tables

In MySQL Workbench or command line:

```sql
-- Check probation periods
SELECT * FROM v_employee_probation;

-- Check leave balances
SELECT * FROM v_leave_balance_summary;

-- Check your employee Sachin
SELECT * FROM probation_periods WHERE employee_id = 13;
```

You should see:
- **Sachin Kumar**: Status = PROBATION, Paid Leave Eligible = FALSE
- PF Percentage = 12.0%
- Annual Tax = (if set, else 0)
- Monthly TDS = Auto-calculated

---

### Step 3: Restart Application

```powershell
# Stop current app (Ctrl+C if running)
cd c:\Users\GCV\Projects\hrmsystem
./mvnw spring-boot:run
```

Wait for: `Started HrmsystemApplication in X.XXX seconds`

---

## 🎯 Test Each Feature

### ✅ Test 1: View Employee Probation Status

**Browser:** `http://localhost:8080/employees`

Find **Sachin Kumar** and look for:
- Probation badge/tag
- End date display
- "Not eligible for paid leaves" message

---

### ✅ Test 2: Try to Apply Leave (Should Be Blocked During Probation)

1. **Login as Sachin** (email: skvenego@gmail.com)
2. **Go to Leaves → Apply Leave**
3. **Try to submit leave application**

**Expected Result:**
```
⚠️ WARNING: You are currently in probation period.
Paid leaves are not applicable during probation.
Remaining probation: X days

[ ] I understand and want to apply for UNPAID LEAVE
```

---

### ✅ Test 3: HR Views Probation Dashboard

1. **Login as HR Admin**
2. **Look for new menu: "Probation"** or **"Confirmations"**
3. **See list of employees in probation**
4. **Click on Sachin's record**

Should show:
```
Employee: Sachin Kumar
Probation Start: 2026-02-05
Probation End: 2026-08-05
Days Remaining: XX
Status: PROBATION
Paid Leave Eligible: NO
PF Rate: 12%
Monthly TDS: ₹XXX

Actions:
[Extend Probation] [Confirm Employee] [View Details]
```

---

### ✅ Test 4: Confirm Employee After Probation

**As HR:**
1. Click **"Confirm Employee"** on Sachin's record
2. Enter confirmation details
3. Submit

**Expected Result:**
- Status changes to CONFIRMED
- Paid Leave Eligible: YES
- Notification sent to employee
- Leave accrual will start next month

**SQL Verification:**
```sql
UPDATE probation_periods 
SET status = 'CONFIRMED', 
    paid_leave_eligible = TRUE,
    confirmed_by = 'HR Manager',
    confirmed_date = CURDATE()
WHERE employee_id = 13;
```

---

### ✅ Test 5: Monthly Leave Accrual (Automatic)

**After confirming Sachin**, wait for 1st of next month OR manually trigger:

```java
// In your code, call this method:
leaveAccrualService.processMonthlyLeaveAccrual();
```

**Expected Result for Sachin:**
```
Leave Balance:
- Opening: 0.0 days
- Earned: 1.5 days (monthly accrual)
- Used: 0.0 days
- Remaining: 1.5 days ✅
```

**Transaction History:**
```
Date: 2026-04-01
Type: EARNED
Days: +1.5
Balance: 1.5
Notes: "Monthly accrual - April 2026"
```

---

### ✅ Test 6: Apply Leave After Probation

1. **Login as Sachin** (now confirmed employee)
2. **Go to Leaves → Apply Leave**
3. **Apply for 1 day** (e.g., April 15, 2026)
4. **Submit**

**Expected Flow:**
```
Before Application:
- Balance: 1.5 days
- Available: 1.5 days ✅

Application Submitted:
- Requested: 1 day
- Dates: April 15, 2026
- Type: Casual Leave

After Approval:
- Balance: 0.5 days
- Transaction recorded
```

---

### ✅ Test 7: View Professional Payslip

1. **Generate payroll for March/April 2026**
2. **Mark as PAID**
3. **Login as Sachin → View Payslip**

**Expected Payslip Shows:**
```
EARNINGS:
- Basic Salary: ₹9,600
- HRA: ₹3,200
- DA: ₹2,400
- Other: ₹800
Total: ₹16,000

DEDUCTIONS:
- Employee PF: ₹1,152 (12%)
- Professional Tax: ₹200
- TDS: ₹XXX (if annual tax entered)
Total Deductions: ₹X,XXX

NET SALARY: ₹XX,XXX

Probation Status: CONFIRMED
Leave Balance: 1.5 days
```

---

## 🔧 Troubleshooting

### Issue 1: Tables Not Created

**Error:** `Table 'probation_periods' doesn't exist`

**Solution:**
```sql
-- Run migration again
SOURCE c:/Users/GCV/Projects/hrmsystem/database_migration.sql;

-- Or manually create
SHOW TABLES; -- Verify tables exist
DESC probation_periods; -- Check structure
```

---

### Issue 2: No Data Showing

**Error:** Probation dashboard empty

**Solution:**
```sql
-- Check if data exists
SELECT COUNT(*) FROM probation_periods;

-- If 0, re-run initialization
INSERT INTO probation_periods (...)
SELECT ... FROM employees e
WHERE e.id NOT IN (SELECT employee_id FROM probation_periods);
```

---

### Issue 3: Leave Not Accruing

**Error:** Balance still 0 after month start

**Check:**
```sql
-- Is employee confirmed?
SELECT paid_leave_eligible FROM probation_periods WHERE employee_id = 13;

-- Should return: TRUE

-- Check if balance record exists
SELECT * FROM leave_balances_enhanced 
WHERE employee_id = 13 AND year = 2026;
```

**Manual Accrual (for testing):**
```sql
UPDATE leave_balances_enhanced 
SET earned_leaves = 1.5, 
    total_available = 1.5,
    remaining_leaves = 1.5
WHERE employee_id = 13 AND year = 2026;

-- Add transaction
INSERT INTO leave_transactions (leave_balance_id, transaction_type, days, balance_after, notes)
SELECT id, 'EARNED', 1.5, 1.5, 'Manual accrual for testing'
FROM leave_balances_enhanced WHERE employee_id = 13 AND year = 2026;
```

---

### Issue 4: TDS Not Calculating

**Error:** Monthly TDS shows 0

**Solution:**
```sql
-- Set annual tax for employee
UPDATE probation_periods 
SET annual_tax = 12000,  -- ₹12,000 per year
    monthly_tds = 1000   -- ₹1,000 per month (auto-calc: 12000/12)
WHERE employee_id = 13;

-- Verify
SELECT annual_tax, monthly_tds FROM probation_periods WHERE employee_id = 13;
```

---

## 📊 Quick SQL Queries

### Check All Probation Employees:
```sql
SELECT * FROM v_employee_probation;
```

### Check Leave Balances:
```sql
SELECT * FROM v_leave_balance_summary;
```

### Check Transaction History:
```sql
SELECT * FROM v_leave_transactions_detail 
WHERE employee_name LIKE '%Sachin%';
```

### Update Employee Status to Confirmed:
```sql
UPDATE probation_periods 
SET status = 'CONFIRMED',
    paid_leave_eligible = TRUE,
    confirmed_by = 'System Admin',
    confirmed_date = CURDATE()
WHERE employee_id = 13;
```

### Manually Add Leave Accrual:
```sql
-- Update balance
UPDATE leave_balances_enhanced 
SET earned_leaves = earned_leaves + 1.5,
    total_available = total_available + 1.5,
    remaining_leaves = remaining_leaves + 1.5
WHERE employee_id = 13 AND year = 2026;

-- Add transaction
INSERT INTO leave_transactions (leave_balance_id, transaction_type, days, balance_after, notes)
SELECT lbe.id, 'EARNED', 1.5, lbe.remaining_leaves, 'Monthly accrual - Manual'
FROM leave_balances_enhanced lbe
WHERE lbe.employee_id = 13 AND lbe.year = 2026;
```

---

## 🎯 What's Next?

After completing these tests, your system will have:

✅ **Professional leave management**  
✅ **Automated tax calculations**  
✅ **Probation tracking**  
✅ **Role-based permissions**  
✅ **Detailed transaction history**  
✅ **Compliant payslips**  

---

## 📞 Need Help?

If you encounter issues:

1. **Check console logs** for error messages
2. **Verify database tables** exist and have data
3. **Clear browser cache** (Ctrl+Shift+R)
4. **Restart application** completely

Share the specific error message and I'll provide exact fix! 🚀

---

## 🎉 Success Indicators

You'll know everything is working when:

1. ✅ Sachin shows as "In Probation" with end date
2. ✅ Cannot apply paid leave during probation
3. ✅ After confirmation, 1.5 leaves accrue automatically
4. ✅ Leave balance updates with each application
5. ✅ Payslip shows PF, TDS, and leave balance
6. ✅ Transaction history shows all leave movements

**Your HRMS is now ENTERPRISE-READY!** 🎊
