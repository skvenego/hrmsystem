# Data Consistency Audit - Frontend vs Backend

## 🚨 CRITICAL BUG FOUND

### Issue: Inconsistent Sunday Exclusion

**File:** `leave.html` lines 1167-1171

**Current (WRONG):**
```javascript
let overlapDays = 0;
let current = new Date(overlapStart);
while (current <= overlapEnd) {
    overlapDays++;  // ❌ Counts ALL days including Sundays!
    current.setDate(current.getDate() + 1);
}
```

**Should be:**
```javascript
// ❌ Missing Sunday exclusion!
```

**Impact:**
- Frontend shows different leave days than backend
- Multi-month leave calculations are wrong
- Payslip shows incorrect data

---

## 📊 API Endpoints Comparison

### my-leaves.html (Employee View)
| Data | API Endpoint | Calculation |
|------|--------------|-------------|
| Leave List | `/api/leaves/my` | Uses stored totalDays |
| Leave Balance | `/api/leaves/balance/{id}` | Backend calculated |
| Stats | Client-side | ❌ Manual calculation |

### leave.html (Admin View)
| Data | API Endpoint | Calculation |
|------|--------------|-------------|
| Leave List | `/api/leaves` | Uses stored values |
| Attendance | `/api/attendance/employee/{id}` | Direct from DB |
| Stats | Client-side | ❌ Wrong overlap counting |

### payslips.html (Employee Payslip)
| Data | API Endpoint | Calculation |
|------|--------------|-------------|
| Payslip | `/api/payslips/{id}` | Backend calculated |
| Balance | `/api/leaves/balance/{id}` | Backend calculated |

### payroll.html (Admin Payroll)
| Data | API Endpoint | Calculation |
|------|--------------|-------------|
| Payroll | `/api/payroll/generate` | Backend calculated |
| Attendance | `/api/attendance/summary` | Backend calculated |

---

## ✅ FIXES REQUIRED

### 1. leave.html - Add Sunday Exclusion
```javascript
// Fix overlap calculation to exclude Sundays
let overlapDays = countDaysExcludingSundays(overlapStart, overlapEnd);
```

### 2. my-leaves.html - Use Backend Calculation
- Remove client-side leave day counting
- Use values from API directly

### 3. Standardize All Pages
- Employee pages: Read-only from API
- Admin pages: Same API, different UI
- Accountant pages: Payroll API only

---

## 🔧 SOLUTION ARCHITECTURE

### Single Source of Truth:
```
┌─────────────────┐
│  AttendanceEngine │ ← Backend calculation
│  (Java)         │
└────────┬────────┘
         │
    ┌────┴────┬────────┬────────┐
    ▼         ▼        ▼        ▼
┌───────┐ ┌──────┐ ┌──────┐ ┌──────┐
│my-    │ │leave │ │payslip│ │payroll│
│leaves │ │.html │ │.html │ │.html │
│       │ │      │ │      │ │      │
└───────┘ └──────┘ └──────┘ └──────┘
   All use same API data
```

### API Consistency:
- All pages use `/api/leaves/balance/{id}` for balance
- All pages use `/api/attendance/employee/{id}` for attendance
- All calculations done in AttendanceEngine
