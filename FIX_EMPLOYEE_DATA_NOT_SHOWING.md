# 🔧 Fix: Employee Data Not Showing on Pages

## 🐛 Problem Diagnosis

**Issue:** Employee pages are blank/not showing data

**Root Causes:**
1. ✅ Security config requires authentication for `/api/employees/**` 
2. ✅ Frontend may not be sending JWT token
3. ✅ Employee data might not exist in database

---

## ✅ Solution 1: Allow Public Access to Employee APIs (For Testing)

Update `SecurityConfig.java` to permit employee API access:

```java
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/api/employees/**").permitAll() // ADD THIS LINE
.requestMatchers("/api/departments/**").permitAll() // ADD THIS LINE
```

---

## ✅ Solution 2: Send JWT Token with Requests

The Employees page SHOULD send the auth token. Check if it's doing so:

### Current Code (Employees.jsx line ~74):
```javascript
const response = await fetch(`/api/employees?_t=${Date.now()}`, {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json'
    // MISSING: Authorization header!
  }
});
```

### Fixed Code:
```javascript
const response = await fetch(`/api/employees?_t=${Date.now()}`, {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('token')}` // ADD THIS
  }
});
```

---

## ✅ Solution 3: Verify Employee Data Exists in Database

Run this SQL query to check:

```sql
SELECT * FROM employees;
```

If no results, you need to insert employee data first!

### Insert Sample Employees:
```sql
INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, status, address) VALUES
('John', 'Doe', 'john.doe@company.com', '+1234567890', 1, 'Software Engineer', '2024-01-15', 75000.00, 'ACTIVE', '123 Main St'),
('Jane', 'Smith', 'jane.smith@company.com', '+0987654321', 2, 'HR Manager', '2023-06-01', 85000.00, 'ACTIVE', '456 Oak Ave'),
('Bob', 'Wilson', 'bob.wilson@company.com', '+1122334455', 1, 'Senior Developer', '2022-03-20', 95000.00, 'ACTIVE', '789 Pine Rd');
```

---

## ✅ Solution 4: Update All API Calls in Employees Page

Search for ALL fetch calls in Employees.jsx and add Authorization header:

### Pattern to Find:
```javascript
await fetch('/api/...', { ... })
```

### Add Headers:
```javascript
await fetch('/api/...', {
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
```

---

## 🚀 Quick Fix Steps

### Step 1: Check Database
```bash
# Login to MySQL
mysql -u root -p hrmsystem

# Check employees table
SELECT COUNT(*) FROM employees;
```

### Step 2: If Count = 0, Insert Data
```sql
INSERT INTO employees (first_name, last_name, email, phone, designation, joining_date, salary, status)
VALUES 
('Test', 'Employee', 'test@company.com', '+1234567890', 'Developer', '2024-01-01', 50000, 'ACTIVE');
```

### Step 3: Update SecurityConfig (Temporary for Testing)
```java
// In SecurityConfig.java line 56-57
.requestMatchers("/api/auth/**").permitAll()
.requestMatchers("/api/employees/**").permitAll()  // ADD THIS
```

### Step 4: Restart Application
```bash
.\mvnw.cmd spring-boot:run
```

### Step 5: Test Employee Page
1. Login to application
2. Navigate to Employees page
3. Should see data now!

---

## 🔍 Detailed Debugging

### Check Browser Console
1. Open DevTools (F12)
2. Go to Network tab
3. Refresh Employees page
4. Look for `/api/employees` request
5. Check:
   - Status code (should be 200)
   - Request headers (should have Authorization)
   - Response body (should have employee data)

### Common Errors

**401 Unauthorized:**
- Token missing or expired
- Solution: Login again, ensure token is in localStorage

**403 Forbidden:**
- Token invalid or insufficient permissions
- Solution: Check JWT token validity

**200 OK but Empty Array:**
- No employee data in database
- Solution: Insert employee records

**CORS Error:**
- Backend CORS misconfigured
- Solution: Check `@CrossOrigin` annotation on controller

---

## 📝 Files to Modify

### 1. SecurityConfig.java (Optional - for easier testing)
```java
@GetMapping
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configure(http))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/employees/**").permitAll()  // ADD
            .requestMatchers("/api/departments/**").permitAll() // ADD
            .requestMatchers("/", "/index.html", "/static/**").permitAll()
            .requestMatchers("/login", "/register").permitAll()
            .requestMatchers("/dashboard/**").permitAll()
            .anyRequest().authenticated()
        )
        // ... rest of config
}
```

### 2. Employees.jsx (ALL fetch calls need auth header)

Find these patterns and update:

**Line ~74 (fetch employees):**
```javascript
const response = await fetch(`/api/employees?_t=${Date.now()}`, {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});
```

**Line ~141 (fetch by ID):**
```javascript
const response = await fetch(`/api/employees/${selectedEmployee.id}`, {
  method: 'GET',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
});
```

**Line ~487 (create employee):**
```javascript
const response = await fetch('/api/employees', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  },
  body: JSON.stringify(employeeData)
});
```

**Similar updates needed for PUT and DELETE calls.**

---

## ✅ Recommended Approach

**For Development:**
1. ✅ Allow `/api/employees/**` without auth (easier testing)
2. ✅ Insert sample employee data
3. ✅ Test all CRUD operations

**For Production:**
1. ❌ Remove public access to employee APIs
2. ✅ Implement proper JWT authentication on all requests
3. ✅ Add role-based access control (ADMIN/HR only)

---

## 🎯 Expected Behavior After Fix

### Employees Page Should Show:
- ✅ List of all employees from database
- ✅ Search functionality
- ✅ Filter by department
- ✅ Add/Edit/Delete buttons
- ✅ View details modal

### Data Flow:
```
Frontend (Employees.jsx)
    ↓ GET /api/employees
    ↓ Headers: Authorization: Bearer <token>
Backend (EmployeeController)
    ↓ @GetMapping
    ↓ employeeService.getAllEmployees()
Database (employees table)
    ↓ SELECT * FROM employees
    ↓ Returns list of employees
Frontend displays data in table/cards
```

---

## 🧪 Testing Checklist

- [ ] Employee page loads without errors
- [ ] Employee list shows data (not empty)
- [ ] Search works
- [ ] Filter by department works
- [ ] "Add Employee" button opens modal
- [ ] Can create new employee
- [ ] Can edit existing employee
- [ ] Can delete employee
- [ ] Can view employee details
- [ ] Browser console has no errors

---

## 📞 Still Not Working?

If after trying all solutions above, still no data:

1. **Check Backend Logs:**
   ```bash
   tail -f logs/hrmsystem.log
   ```

2. **Verify Database Connection:**
   ```sql
   SHOW TABLES;
   DESCRIBE employees;
   ```

3. **Test API Directly:**
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN_HERE" http://localhost:8080/api/employees
   ```

4. **Check Token Validity:**
   ```javascript
   console.log('Token:', localStorage.getItem('token'));
   // Should not be null
   ```

---

**Most Likely Issue:** Employee data doesn't exist in database!

**Quick Fix:** Run the INSERT statement in Solution 3 above.
