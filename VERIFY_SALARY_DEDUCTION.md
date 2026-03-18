# ✅ How to Verify Salary Deduction is Showing in Your Payslip

## Quick Test Steps:

### Step 1: Open Your Browser
- URL: `http://localhost:8080`
- Login with your credentials (admin/admin123)

### Step 2: Navigate to Payroll Page
- Click on **"Payroll"** in the left sidebar

### Step 3: Generate Payroll (if not already done)
- Click **"Generate Payroll"** button
- Select Month: **3** (March)
- Select Year: **2026**
- Click **"Generate"**

### Step 4: View Any Employee's Payslip
- Find any employee in the list (e.g., Sachin Verma, Ram Murat, etc.)
- Click the **"View"** button next to their name

---

## What You Should See:

If the employee has **absent days OR leave days**, you will see a NEW section with yellow background:

```
┌─────────────────────────────────────────────┐
│ 💰 Salary Calculation Breakdown             │ ← YELLOW BOX
├─────────────────────────────────────────────┤
│                                             │
│ Per Day Basic Salary:                       │
│   ₹1,666.67 per day                        │ ← Green text
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Absent Days (5 days):                   │ │ ← RED BOX
│ │   -₹8,333.35                            │ │
│ │   Deducted from salary for absence      │ │
│ └─────────────────────────────────────────┘ │
│                                             │
│ ┌─────────────────────────────────────────┐ │
│ │ Leave Days (3 days):                    │ │ ← YELLOW BOX
│ │   PAID (Approved Leave)                 │ │
│ │   No deduction - included in salary     │ │
│ └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

## Verification Checklist:

### ✅ You WILL see the breakdown IF:
- Employee has **absentDays > 0** (example: 5 absent days)
- OR employee has **leaveDays > 0** (example: 3 approved leave days)

### ❌ You WON'T see the breakdown IF:
- Employee has **perfect attendance** (no absent days, no leave days)
- Example: Present: 26, Absent: 0, Leave: 0

---

## Test Scenarios:

### Scenario 1: Employee with Absent Days
**Test Data:**
- Name: Sachin Verma
- Present: 20 days
- Absent: 5 days
- Leave: 0 days
- Basic Salary: ₹50,000

**Expected Result:**
```
💰 Salary Calculation Breakdown

Per Day Basic Salary:
  ₹1,666.67 per day

┌──────────────────────────────┐
│ Absent Days (5 days):        │
│   -₹8,333.35                 │
│   Deducted from salary       │
└──────────────────────────────┘
```

✅ **You should see the RED box showing money deducted!**

---

### Scenario 2: Employee with Approved Leave
**Test Data:**
- Name: Ram Murat
- Present: 23 days
- Absent: 0 days
- Leave: 3 days (approved)
- Basic Salary: ₹45,000

**Expected Result:**
```
💰 Salary Calculation Breakdown

Per Day Basic Salary:
  ₹1,500.00 per day

┌──────────────────────────────┐
│ Leave Days (3 days):         │
│   PAID (Approved Leave)      │
│   No deduction               │
└──────────────────────────────┘
```

✅ **You should see the YELLOW box showing "PAID"!**

---

### Scenario 3: Employee with Both Absent + Leave
**Test Data:**
- Name: Prem Prajapati
- Present: 18 days
- Absent: 4 days
- Leave: 5 days (approved)
- Basic Salary: ₹55,000

**Expected Result:**
```
💰 Salary Calculation Breakdown

Per Day Basic Salary:
  ₹1,833.33 per day

┌──────────────────────────────┐
│ Absent Days (4 days):        │
│   -₹7,333.32                 │
│   Deducted from salary       │
└──────────────────────────────┘

┌──────────────────────────────┐
│ Leave Days (5 days):         │
│   PAID (Approved Leave)      │
│   No deduction               │
└──────────────────────────────┘
```

✅ **You should see BOTH boxes!**

---

### Scenario 4: Employee with Perfect Attendance
**Test Data:**
- Name: Deepak Rajpoot
- Present: 26 days
- Absent: 0 days
- Leave: 0 days
- Basic Salary: ₹60,000

**Expected Result:**
```
[NO Salary Calculation Breakdown section]
```

❌ **The breakdown section will NOT appear because there are no deductions!**

---

## Where It Appears in the Payslip:

The complete payslip layout:

```
┌─────────────────────────────────────┐
│     Payslip Details                 │
├─────────────────────────────────────┤
│ Employee: Sachin Verma              │
│ Department: IT                      │
│ Period: March 2026                  │
├─────────────────────────────────────┤
│ Present: 20  Absent: 5  Leave: 3    │
├─────────────────────────────────────┤
│ Earnings          │ Deductions      │
│ Basic: ₹50,000   │ PF: ₹6,000      │
│ HRA: ₹20,000     │ Tax: ₹5,000     │
│ Gross: ₹80,000   │ Total: ₹11,000  │
├─────────────────────────────────────┤
│ 💰 Salary Calculation Breakdown     │ ← NEW SECTION!
│ Per Day: ₹1,666.67                 │
│ Absent (5): -₹8,333.35             │
│ Leave (3): PAID                    │
├─────────────────────────────────────┤
│ Absent Dates Details                │
│ 2026-03-05, 2026-03-12, ...        │
├─────────────────────────────────────┤
│ Net Salary: ₹60,666.65             │
└─────────────────────────────────────┘
```

---

## Color Guide:

- 🟨 **Yellow Background Box** - Salary Calculation Breakdown section
- 🟩 **Green Text** - Per day salary amount
- 🟥 **Red Background Box** - Absent days deduction (money lost)
- 🟨 **Yellow Background Box** - Leave days info (paid)

---

## If You DON'T See the Breakdown:

### Check These:

1. **Does the employee have absent or leave days?**
   - If Absent = 0 AND Leave = 0 → Breakdown won't show (that's correct!)
   - Need at least 1 absent day OR 1 leave day to trigger display

2. **Did you refresh the browser?**
   - Press **Ctrl + F5** to clear cache and reload new code

3. **Is payroll generated for current month?**
   - Go to Payroll page → Click "Generate Payroll"
   - Select March 2026 → Click "Generate"

4. **Are you viewing the right modal?**
   - Click "View" button (blue eye icon)
   - NOT "Process", "Mark Paid", etc.

---

## Quick Debug:

If still not showing, open browser console (F12) and check:

1. Click "View" on an employee
2. Look for any JavaScript errors in Console tab
3. Check Network tab for API calls to `/api/payroll/payslip/`
4. Response should include: `absentDays`, `leaveDays`, `basicSalary`

Example response:
```json
{
  "employeeName": "Sachin Verma",
  "basicSalary": 50000,
  "absentDays": 5,
  "leaveDays": 3,
  "presentDays": 20,
  ...
}
```

---

## Success Indicators:

✅ **You'll know it's working when you see:**
- Yellow/gold colored box titled "💰 Salary Calculation Breakdown"
- Shows "Per Day Basic Salary: ₹X,XXX.XX per day"
- Shows red box with "-₹X,XXX.XX" for absent days
- Shows yellow box with "PAID (Approved Leave)" for leave days

---

**Your payslip now shows EXACTLY how much money is deducted for absent days!** 💰✅
