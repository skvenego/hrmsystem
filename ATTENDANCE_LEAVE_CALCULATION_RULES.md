# Attendance & Leave Calculation Rules

## Overview
This document explains all the attendance and leave calculation rules implemented in the HRM System for payslip and payroll generation.

---

## Priority Order (IMPORTANT)

When calculating any day, the system follows this priority order:

1. **Priority 1: Approved Leave** (highest priority)
   - If HR has approved a leave request for the date
   - Leave overrides everything else
   - Manual absent marking is ignored if approved leave exists

2. **Priority 2: Attendance Marking**
   - If no approved leave exists
   - Check the attendance status marked for the date
   - PRESENT, ABSENT, or HALF_DAY

3. **Priority 3: Unmarked Days**
   - If no approved leave AND no attendance marking
   - Day is skipped (not counted as present or absent)

---

## 1. Leave Handling Rules

### When Leave is Approved by HR

**Full-Day Leave:**
- Count as: 1.0 present day
- NOT counted as absent
- Deduction: Based on leave balance (paid or unpaid)

**Half-Day Leave:**
- Count as: 0.5 present day
- NOT counted as absent
- Deduction: Based on leave balance (paid or unpaid)

### Leave Balance Impact

**If Leave Balance Available:**
- Full-day leave → 1.0 paid leave (no salary deduction)
- Half-day leave → 0.5 paid leave (no salary deduction)

**If No Leave Balance:**
- Full-day leave → 1.0 unpaid leave (salary deduction applies)
- Half-day leave → 0.5 unpaid leave (salary deduction applies)

---

## 2. Attendance Marking Rules

### When User Clicks "Present"
- Count as: 1.0 present day
- No absent days

### When User Clicks "Absent"
- Count as: 1.0 absent day
- No present days
- Salary deduction applies (based on daily basic salary)

### When User Clicks "Half Day" (Attendance)
- Count as: 0.5 present day
- Count as: 0.5 absent day
- Salary deduction applies for 0.5 day (based on daily basic salary)

---

## 3. Half-Day Leave Rule

### When User Applies for Half-Day Leave

**If Leave Balance Available:**
- Count as: 0.5 present day
- Count as: 0 absent days
- No salary deduction (treated as paid leave)

**If No Leave Balance:**
- Count as: 0.5 present day
- Count as: 0.5 absent day
- Salary deduction applies for 0.5 day

---

## 4. Sunday Handling

- Sundays are **excluded** from attendance calculation
- Sundays do not count as present or absent days
- Leave that falls on Sunday is still counted as leave

---

## 5. Salary Deduction Calculation

### Daily Rate Calculation
- Daily Basic Salary = Monthly Basic Salary ÷ 26 days
- (26 working days is the standard)

### Deduction Types

**Absent Days Deduction:**
- Deduction = Absent Days × Daily Basic Salary

**Half-Day Deduction (Attendance):**
- Deduction = 0.5 × Daily Basic Salary

**Unpaid Leave Deduction:**
- Deduction = Unpaid Leave Days × Daily Basic Salary

**Total Attendance Deduction:**
- Total = Absent Deduction + Half-Day Deduction + Unpaid Leave Deduction

---

## 6. Summary Table

| Scenario | Present | Absent | Leave | Deduction |
|----------|---------|--------|-------|-----------|
| Full-day present | 1.0 | 0 | 0 | None |
| Full-day absent | 0 | 1.0 | 0 | Full day |
| Attendance half-day | 0.5 | 0.5 | 0 | Half day |
| Full-day leave (paid) | 1.0 | 0 | 1.0 | None |
| Full-day leave (unpaid) | 1.0 | 0 | 1.0 | Full day |
| Half-day leave (paid) | 0.5 | 0 | 0.5 | None |
| Half-day leave (unpaid) | 0.5 | 0 | 0.5 | Half day |

---

## 7. Key Principles

1. **Approved Leave Overrides Absent**
   - If HR approves leave, manual absent marking is ignored
   - This prevents double-counting or incorrect deductions

2. **Leave is Always Counted as Present**
   - Approved leave (full or half) is counted as present days
   - This ensures employees get credit for leave days
   - Deduction is based on leave balance, not attendance

3. **Half-Day Attendance = Half Present + Half Absent**
   - This reflects partial attendance
   - Salary is deducted for the absent portion

4. **Sundays are Excluded**
   - Sundays are not working days
   - They don't affect attendance calculations

---

## Implementation Notes

- All calculations are done using `Double` type for precision
- Half days are represented as 0.5 in calculations
- The priority order is strictly enforced in PayslipService
- PayrollService uses the same calculation logic via PayslipService methods

---

## Files Modified

- `PayslipService.java` - Main calculation logic with priority order
- `PayrollService.java` - Uses PayslipService methods for consistency
- `PayrollDTO.java` - Changed presentDays/absentDays to Double type
- `PayslipDTO.java` - Changed presentDays/absentDays to Double type
- `Payslip.java` entity - Changed presentDays/absentDays to Double type
