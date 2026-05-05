# 🔥 URGENT: Complete Payroll Fix Steps

## Problem
Database has stale payroll records calculated with OLD buggy code.

## Anubhav Sharma Example:
- Present: 4 days
- Should be absent: 22 days (26 - 4)
- Per day: ₹1,308 (₹34,000 ÷ 26)
- **Correct deduction**: 22 × ₹1,308 = ₹28,776
- **Current showing**: ₹34,787 ❌ (WRONG - from old double deduction bug)

---

## 🛠️ COMPLETE FIX STEPS

### STEP 1: Stop Server
Already done (exit code 1)

### STEP 2: Clean Build
```bash
./mvnw clean compile
```

### STEP 3: Clear Stale Database Records
Run this SQL in MySQL:
```sql
-- Delete ALL April 2026 payroll records
DELETE FROM payroll WHERE month = 4 AND year = 2026;

-- Also clear related payslips if needed
DELETE FROM payslip WHERE month = 4 AND year = 2026;
```

### STEP 4: Restart Server
```bash
./mvnw spring-boot:run
```

### STEP 5: Regenerate Payroll
1. Go to **Payroll** page
2. Select **April 2026**
3. Click **"Generate Payroll for All"** or individual
4. Verify new calculations

---

## ✅ Expected Results After Fix

| Employee | Present | Absent | Per Day | Deduction | Basic | Net (approx) |
|----------|---------|--------|---------|-----------|-------|--------------|
| Anubhav Sharma | 4 | 22 | ₹1,308 | ₹28,776 | ₹34,000 | ₹5,200 |
| Akash Tiwari | ? | ? | ₹1,346 | ? | ₹35,000 | ? |

**Note**: Net = Basic + Allowances - (PF + Tax + Insurance + Attendance Deduction)

---

## 🔍 Debug Commands

If still wrong, check logs for:
```
AttendanceEngine Result for Employee 16: present=4.0, absent=22.0, paidLeave=0.0, unpaidLeave=22.0
Salary Calculation - Basic: 34000, HRA: 200, DA: 100, Other: 300
Attendance Deduction - Total Absent Days: 22.0, Amount: 28776
```
