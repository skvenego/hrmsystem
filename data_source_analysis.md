# Leave Page Data Source Analysis

## Employee: Anubhav Sharma (ID: 16)

---

## 📊 Section 1: Leave Balance

### Displayed Values:
| Field | Showing | Should Show |
|-------|---------|-------------|
| Total Earned | 6.0 | 6.0 ✓ |
| Used | 0.0 | 1.0 ❌ |
| Remaining | 6.0 | 5.0 ❌ |
| Unpaid | 0.0 | 0.0 ✓ |

### API Endpoint:
```
GET /api/leaves/balance/16
```

### Controller:
`LeaveController.java` line 194-198
```java
@GetMapping("/balance/{employeeId}")
public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(@PathVariable Long employeeId) {
    LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
    return ResponseEntity.ok(balance);
}
```

### Service Method:
`LeaveService.java` - `getLeaveBalance()` (around line 285-340)

### Data Flow:
1. **leave_balances** table → cached balance
2. **leaves** table → approved leaves calculation
3. Java calculates: `usedLeaves = sum of approved leaves (excluding Sundays)`

### Why Wrong:
The `usedLeaves` is calculated from `leave.getPaidDays()` (stored value) instead of fresh calculation with Sunday exclusion.

**Already Fixed!** - Now uses `countDaysExcludingSundays()`

---

## 📊 Section 2: Attendance & Leave Summary

### Displayed Values:
| Field | Showing | Correct? |
|-------|---------|----------|
| Present Days | 4.0 | ✓ |
| Absent Days | 0.0 | ✓ |
| Paid Leave Days | 1.0 | ✓ |
| Unpaid Leave Days | 0.0 | ✓ |

### API Endpoint:
```
GET /api/attendance/summary?employeeId=16&month=4&year=2026
```

### Data Sources:
- **attendance** table - present/absent status
- **payroll** table - paid/unpaid leave days (pre-calculated)

### Why Correct:
This uses pre-calculated data from payroll table which already has correct values.

---

## 🔍 Debug Steps

### Step 1: Check Database Values
```sql
-- Run these in MySQL:

-- Check leave_balances (stored balance)
SELECT * FROM leave_balances WHERE employee_id = 16 AND year = 2026 AND cycle = 1;

-- Check approved leaves
SELECT * FROM leaves WHERE employee_id = 16 AND status = 'APPROVED' AND end_date <= '2026-06-30';

-- Check payroll for April
SELECT * FROM payroll WHERE employee_id = 16 AND month = 4 AND year = 2026;

-- Check attendance for April
SELECT * FROM attendance WHERE employee_id = 16 AND date BETWEEN '2026-04-01' AND '2026-04-30';
```

### Step 2: Test API Response
```bash
# Get token first
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'

# Then call balance API
curl http://localhost:8080/api/leaves/balance/16 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Step 3: Expected Response
```json
{
  "totalEarnedLeaves": 6.0,
  "usedLeaves": 1.0,        // <-- Should be 1.0, not 0.0
  "remainingLeaves": 5.0,   // <-- Should be 5.0, not 6.0
  "unpaidLeaves": 0.0,
  "currentCycle": 1,
  "year": 2026
}
```

---

## ✅ After Fix - Restart Required

```bash
./mvnw spring-boot:run
```

Then hard refresh browser (Ctrl+F5)
