# 🔐 ROLE-BASED LOGIN SYSTEM - COMPLETE IMPLEMENTATION

## 📋 Overview

This document describes the complete role-based authentication and authorization system with specific page access permissions for each role.

---

## 👥 User Roles & Permissions

### 1. **BM (Branch Manager)**
**Login:** `bm@company.com`  
**Access Level:** Branch-wide operations

**Can Access:**
- ✅ Dashboard (branch statistics)
- ✅ All Employees (view only)
- ✅ Attendance (mark/approve for branch)
- ✅ Leaves (approve/reject for branch)
- ✅ Reports (branch performance)
- ❌ Payroll (read-only access)
- ❌ Employee Management (cannot add/delete)

---

### 2. **HR (Human Resources)**
**Login:** `hr@company.com`  
**Access Level:** Full employee lifecycle

**Can Access:**
- ✅ Dashboard (all employees)
- ✅ All Employees (CRUD operations)
- ✅ Attendance (manage)
- ✅ Leaves (approve/reject)
- ✅ Probation Management (confirm/extend)
- ✅ Recruitment
- ❌ Payroll (limited access)

---

### 3. **Accountant**
**Login:** `accountant@company.com`  
**Access Level:** Finance & Payroll

**Can Access:**
- ✅ Dashboard (financial stats)
- ✅ Payroll (process/manage)
- ✅ PF Management
- ✅ Tax Management (TDS calculations)
- ✅ Payslips (generate/view)
- ✅ Financial Reports
- ❌ Employee Management
- ❌ Leave Approval

---

### 4. **Director**
**Login:** `director@company.com`  
**Access Level:** Strategic oversight

**Can Access:**
- ✅ Dashboard (analytics & KPIs)
- ✅ All Reports (company-wide)
- ✅ Analytics
- ✅ Department Performance
- ✅ Payroll Overview (read-only)
- ✅ Employee Strength Reports
- ❌ Operational Tasks

---

### 5. **Employee**
**Login:** `employee@company.com` (e.g., skvenego@gmail.com)  
**Access Level:** Self-service only

**Can Access:**
- ✅ My Dashboard (personal stats)
- ✅ My Profile (view/edit)
- ✅ My Leaves (apply/view history)
- ✅ My Attendance (view/mark)
- ✅ My Payslips (view/download)
- ✅ Settings (personal info)
- ❌ All Other Pages

---

## 🏢 EMPLOYEE SELF-SERVICE PORTAL - DETAILED FEATURES

### **After Login, Employee Can:**

#### **1. Dashboard (Personal)**
Shows:
- Total working days this month
- Present days
- Absent days
- Leave balance
- Upcoming holidays
- Recent notifications

---

#### **2. My Leaves Page**

##### **Section A: Leave Balance Card**
```
┌─────────────────────────────────────────┐
│  LEAVE BALANCE 2026                     │
├─────────────────────────────────────────┤
│  Semester 1 (Jan - Jun)                │
│  ─────────────────────────────────────  │
│  Earned:      9.0 days                  │
│  Used:        3.0 days                  │
│  Remaining:   6.0 days ⚠️               │
│  Expires on:  30-Jun-2026              │
│                                         │
│  Semester 2 (Jul - Dec)                │
│  ─────────────────────────────────────  │
│  Earned:      9.0 days                  │
│  Used:        0.0 days                  │
│  Remaining:   9.0 days                  │
│  Expires on:  31-Dec-2026              │
│                                         │
│  TOTAL AVAILABLE: 15.0 days            │
└─────────────────────────────────────────┘
```

##### **Section B: Apply for Leave**
**IF in Probation Period:**
```
╔═════════════════════════════════════════╗
║  ⚠️ PROBATION PERIOD                    ║
╠═════════════════════════════════════════╣
║  You are currently in probation period. ║
║  Paid leaves are NOT applicable.        ║
║                                         ║
║  Probation End Date: 05-Aug-2026       ║
║  Remaining Days: 45                     ║
║                                         ║
║  [ ] I understand and want to apply    ║
║      for UNPAID LEAVE instead          ║
║                                         ║
║  [Submit Unpaid Leave Request]         ║
╚═════════════════════════════════════════╝
```

**IF Confirmed Employee:**
```
┌─────────────────────────────────────────┐
│  APPLY FOR LEAVE                        │
├─────────────────────────────────────────┤
│  Leave Type:  [Casual ▼]               │
│  From Date:   [15-Apr-2026]            │
│  To Date:     [17-Apr-2026]            │
│  Total Days:  3.0 days                  │
│  Reason:      [Medical treatment...]   │
│                                         │
│  Available Balance: 15.0 days ✅        │
│                                         │
│  [Submit Leave Application]            │
└─────────────────────────────────────────┘
```

##### **Section C: Leave History**
```
┌─────────────────────────────────────────┐
│  LEAVE HISTORY                          │
├─────────────────────────────────────────┤
│  # | Type  | Dates         | Status    │
│  ──┼───────┼───────────────┼───────────│
│  1 | Sick  | 10-Mar-2026   | ✅ Approved│
│    |       | (2 days)      |           │
│  2 | Casual| 15-Feb-2026   | ✅ Approved│
│    |       | (1 day)       |           │
│  3 | Annual| 20-Jan-2026   | ❌ Rejected│
│    |       | (3 days)      | Pending   │
└─────────────────────────────────────────┘
```

##### **Section D: Pending Leaves (Already Taken)**
```
┌─────────────────────────────────────────┐
│  PENDING LEAVES (Applied, Not Approved) │
├─────────────────────────────────────────┤
│  Application #123                       │
│  Type: Casual Leave                     │
│  Dates: 25-Apr-2026 to 26-Apr-2026     │
│  Total Days: 2.0                        │
│  Applied On: 20-Apr-2026               │
│  Status: ⏳ Pending Manager Approval    │
│                                         │
│  Application #124                       │
│  Type: Sick Leave                       │
│  Dates: 02-May-2026                     │
│  Total Days: 1.0                        │
│  Applied On: 28-Apr-2026               │
│  Status: ⏳ Pending HR Approval         │
└─────────────────────────────────────────┘
```

---

#### **3. My Attendance Page**
```
┌─────────────────────────────────────────┐
│  MY ATTENDANCE - April 2026             │
├─────────────────────────────────────────┤
│  Present:    18 days                    │
│  Absent:     2 days                     │
│  Late:       1 day                      │
│  Half Day:   1 day                      │
│  Leave:      3 days                     │
│  Holiday:    6 days                     │
│                                         │
│  [Check-In] [Check-Out]                │
│                                         │
│  Daily Log:                             │
│  ─────────────────────────────────────  │
│  01-Apr: Present  09:00 - 18:00        │
│  02-Apr: Present  09:15 - 18:15 (Late) │
│  03-Apr: Leave    (Sick Leave)         │
│  04-Apr: Holiday  (Sunday)             │
│  ...                                    │
└─────────────────────────────────────────┘
```

---

#### **4. My Payslips Page**
```
┌─────────────────────────────────────────┐
│  MY PAYSLIPS                            │
├─────────────────────────────────────────┤
│  March 2026                             │
│  Net Salary: ₹13,648                    │
│  Status: ✅ PAID (01-Apr-2026)         │
│  [Download PDF] [View Details]         │
│                                         │
│  February 2026                          │
│  Net Salary: ₹13,648                    │
│  Status: ✅ PAID (01-Mar-2026)         │
│  [Download PDF] [View Details]         │
│                                         │
│  January 2026                           │
│  Net Salary: ₹13,648                    │
│  Status: ✅ PAID (01-Feb-2026)         │
│  [Download PDF] [View Details]         │
└─────────────────────────────────────────┘

Click "View Details" shows:
┌─────────────────────────────────────────┐
│  PAYSLIP DETAILS - March 2026           │
├─────────────────────────────────────────┤
│  EARNINGS:                              │
│  Basic Salary:    ₹9,600.00            │
│  HRA:             ₹3,200.00            │
│  DA:              ₹2,400.00            │
│  Other Allow.:      ₹800.00            │
│  ─────────────────────────────────────  │
│  Gross Earnings: ₹16,000.00            │
│                                         │
│  DEDUCTIONS:                            │
│  Employee PF:     ₹1,152.00 (12%)      │
│  Professional Tax:  ₹200.00            │
│  TDS:             ₹1,000.00            │
│  ─────────────────────────────────────  │
│  Total Deduct.:   ₹2,352.00            │
│                                         │
│  NET SALARY:      ₹13,648.00           │
│                                         │
│  Probation Status: CONFIRMED ✅        │
│  Leave Balance:   15.0 days            │
└─────────────────────────────────────────┘
```

---

#### **5. My Profile Page**
```
┌─────────────────────────────────────────┐
│  MY PROFILE                             │
├─────────────────────────────────────────┤
│  Personal Information                   │
│  ─────────────────────────────────────  │
│  Name:          Sachin Kumar            │
│  Email:         skvenego@gmail.com      │
│  Phone:         9555879330              │
│  Address:       [Edit...]              │
│                                         │
│  Employment Details                     │
│  ─────────────────────────────────────  │
│  Employee ID:   EMP013                  │
│  Department:    IT                      │
│  Designation:   Full Stack Developer    │
│  Joining Date:  05-Feb-2026            │
│  Status:        ACTIVE                  │
│                                         │
│  Probation Information                  │
│  ─────────────────────────────────────  │
│  Status:        CONFIRMED ✅            │
│  Start Date:    05-Feb-2026            │
│  End Date:      05-Aug-2026            │
│  Confirmed By:  HR Manager              │
│                                         │
│  [Edit Profile] [Change Password]      │
└─────────────────────────────────────────┘
```

---

#### **6. Settings Page**
```
┌─────────────────────────────────────────┐
│  SETTINGS                               │
├─────────────────────────────────────────┤
│  Notification Preferences               │
│  ─────────────────────────────────────  │
│  📧 Email Notifications:  [ON]         │
│  🔔 Push Notifications:   [ON]         │
│  📱 SMS Notifications:     [OFF]       │
│                                         │
│  Leave Updates:          [ON]          │
│  Payroll Updates:        [ON]          │
│  Attendance Updates:     [ON]          │
│                                         │
│  [Save Preferences]                    │
└─────────────────────────────────────────┘
```

---

## 🗓️ SEMESTER-WISE LEAVE EXPIRY SYSTEM

### **Policy:**

```
Year 2026 Divided into 2 Semesters:

SEMESTER 1: January - June
- Earned Leaves: 9.0 days (1.5 × 6 months)
- Expires On: 30-Jun-2026
- Unused leaves lapse OR carry forward (max 30 days)

SEMESTER 2: July - December
- Earned Leaves: 9.0 days (1.5 × 6 months)
- Expires On: 31-Dec-2026
- Unused leaves lapse OR carry forward (max 30 days)
```

### **Employee Display Example:**

```sql
-- Database Structure
CREATE TABLE leave_semesters (
    id BIGINT PRIMARY KEY,
    employee_id BIGINT,
    year INT,
    semester ENUM('S1', 'S2'),
    start_month VARCHAR(10), -- 'January' or 'July'
    end_month VARCHAR(10),   -- 'June' or 'December'
    earned_leaves DOUBLE,
    used_leaves DOUBLE,
    remaining_leaves DOUBLE,
    expiry_date DATE,
    carried_forward DOUBLE,
    lapsed_leaves DOUBLE
);
```

### **Visual Representation for Employee:**

```
┌─────────────────────────────────────────────────┐
│  LEAVE YEAR 2026                                │
├─────────────────────────────────────────────────┤
│                                                 │
│  📅 SEMESTER 1: Jan - Jun 2026                 │
│  ═══════════════════════════════════════════   │
│  Earned:    ████████░░ 9.0 days                │
│  Used:      ████░░░░░░ 4.0 days                │
│  Remaining: ████░░░░░░ 5.0 days ⚠️             │
│                                                 │
│  ⚠️ EXPIRES IN: 45 DAYS (30-Jun-2026)         │
│  Use it or lose it! (Or carry forward max 30)  │
│                                                 │
├─────────────────────────────────────────────────┤
│                                                 │
│  📅 SEMESTER 2: Jul - Dec 2026                 │
│  ═══════════════════════════════════════════   │
│  Earned:    ████████░░ 9.0 days                │
│  Used:      ██░░░░░░░░ 2.0 days                │
│  Remaining: ██████░░░░ 7.0 days                │
│                                                 │
│  ⏰ EXPIRES ON: 31-Dec-2026                    │
│  Plenty of time remaining!                      │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 🔒 PAGE ACCESS MATRIX

| Page | BM | HR | Accountant | Director | Employee |
|------|----|----|------------|----------|----------|
| **Dashboard** | ✅ Full | ✅ Full | ✅ Finance | ✅ Reports | ✅ Personal |
| **Employees** | ✅ View | ✅ CRUD | ❌ | ✅ View | ❌ |
| **Attendance** | ✅ Approve | ✅ Manage | ❌ | ✅ View | ✅ Self |
| **Leaves** | ✅ Approve | ✅ Approve | ❌ | ✅ View | ✅ Apply |
| **Payroll** | ✅ View | ❌ | ✅ Process | ✅ View | ✅ View Own |
| **Payslips** | ❌ | ✅ All | ✅ All | ✅ View | ✅ Own |
| **Reports** | ✅ Branch | ✅ All | ✅ Finance | ✅ All | ❌ |
| **Settings** | ✅ | ✅ | ✅ | ✅ | ✅ Personal |
| **Probation** | ✅ View | ✅ Manage | ❌ | ✅ View | ❌ |
| **Departments** | ✅ | ✅ | ❌ | ✅ | ❌ |

---

## 🎯 IMPLEMENTATION STEPS

### Step 1: Update User Model

Add role enumeration:

```java
public enum Role {
    ROLE_BM("Branch Manager"),
    ROLE_HR("Human Resources"),
    ROLE_ACCOUNTANT("Accountant"),
    ROLE_DIRECTOR("Director"),
    ROLE_EMPLOYEE("Employee");
    
    private final String displayName;
    
    Role(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

### Step 2: Create Role-Based Guard Service

```java
@Service
public class RoleGuardService {
    
    public boolean hasAccess(User user, String page) {
        switch (user.getRole()) {
            case ROLE_BM:
                return canAccessBM(page);
            case ROLE_HR:
                return canAccessHR(page);
            case ROLE_ACCOUNTANT:
                return canAccessAccountant(page);
            case ROLE_DIRECTOR:
                return canAccessDirector(page);
            case ROLE_EMPLOYEE:
                return canAccessEmployee(page);
            default:
                return false;
        }
    }
    
    private boolean canAccessEmployee(String page) {
        return Arrays.asList(
            "dashboard",
            "my-profile",
            "my-leaves",
            "my-attendance",
            "my-payslips",
            "settings"
        ).contains(page);
    }
    
    // ... other role methods
}
```

### Step 3: Frontend Route Protection

```javascript
// ProtectedRoute.jsx
const ProtectedRoute = ({ children, allowedRoles }) => {
  const { user } = useAuth();
  
  if (!allowedRoles.includes(user.role)) {
    return <Navigate to="/unauthorized" />;
  }
  
  return children;
};

// Usage in App.jsx
<Route 
  path="/employees" 
  element={
    <ProtectedRoute allowedRoles={['ROLE_BM', 'ROLE_HR', 'ROLE_DIRECTOR']}>
      <EmployeesPage />
    </ProtectedRoute>
  } 
/>

<Route 
  path="/my-leaves" 
  element={
    <ProtectedRoute allowedRoles={['ROLE_EMPLOYEE', 'ROLE_HR']}>
      <EmployeeLeavesPage />
    </ProtectedRoute>
  } 
/>
```

---

## 📊 Summary

### **Professional Workflow:**

1. **User logs in** → System checks role
2. **Based on role** → Shows specific menu/pages
3. **Employee sees:**
   - Only personal dashboard
   - Only own leaves (balance + history)
   - Only own attendance
   - Only own payslips
   - Cannot access admin pages

4. **Leave System:**
   - During probation: No paid leaves
   - After confirmation: 1.5 leaves/month
   - Semester-wise tracking (Jan-Jun, Jul-Dec)
   - Expiry date shown clearly
   - Already taken leaves show dates
   - Pending applications visible

5. **Professional Features:**
   - Clean UI with role-specific menus
   - Leave balance cards with expiry warnings
   - Transaction history with dates
   - Professional payslip with all details
   - Probation status tracking

---

This is how a **professional enterprise HRMS** works! Would you like me to implement the actual code for any specific role or feature?
