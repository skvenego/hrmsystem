# 🗄️ DATABASE MIGRATION GUIDE - DUAL APPROVAL PAYROLL

## 📋 OVERVIEW

This guide will help you run the database migration for the dual-approval payroll system.

---

## ✅ WHAT THE MIGRATION DOES

### **1. Updates `payroll` Table:**
Adds these new columns:
```sql
- requires_dual_approval    BOOLEAN  DEFAULT TRUE
- accountant_approved       BOOLEAN  DEFAULT FALSE
- director_approved         BOOLEAN  DEFAULT FALSE
- final_approval_date       DATE
- payslip_generated         BOOLEAN  DEFAULT FALSE
```

### **2. Creates New Table `payroll_approvals`:**
```sql
CREATE TABLE payroll_approvals (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    payroll_id      BIGINT NOT NULL,           -- FK to payroll
    approver_role   ENUM('ROLE_ACCOUNTANT', 'ROLE_DIRECTOR'),
    approved_by     BIGINT,                     -- FK to users
    status          ENUM('PENDING', 'APPROVED', 'REJECTED'),
    approved_at     TIMESTAMP,
    rejection_reason VARCHAR(500),
    comments        VARCHAR(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### **3. Initializes Existing Data:**
- Sets default values for all existing payrolls
- Optionally creates approval records for already-paid payrolls

---

## 🚀 HOW TO RUN THE MIGRATION

### **Method 1: Using MySQL Workbench (RECOMMENDED)**

#### Step 1: Open MySQL Workbench
1. Launch MySQL Workbench
2. Connect to your local MySQL server
3. Select the `hrmsystem` database

#### Step 2: Open SQL File
1. Click **File → Open SQL Script**
2. Navigate to: `c:\Users\GCV\Projects\hrmsystem\database_migration_dual_approval.sql`
3. Click **Open**

#### Step 3: Execute
1. Click the **⚡ Execute** button (or press Ctrl+Shift+Enter)
2. Wait for completion message
3. You should see: `✅ Dual Approval Payroll Migration Complete!`

---

### **Method 2: Using Command Line**

#### Option A: PowerShell Script
```powershell
# Run the PowerShell script I created
.\run_database_migration.ps1
```

#### Option B: Direct MySQL Command
```bash
# Navigate to project directory
cd c:\Users\GCV\Projects\hrmsystem

# Run migration
mysql -u root -p hrmsystem < database_migration_dual_approval.sql
```

**Note:** If MySQL is not in your PATH, use full path:
```bash
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p hrmsystem < database_migration_dual_approval.sql
```

---

### **Method 3: Manual Execution in MySQL CLI**

#### Step 1: Open MySQL Command Line
```bash
mysql -u root -p
```

#### Step 2: Select Database
```sql
USE hrmsystem;
```

#### Step 3: Copy-Paste SQL Commands
Open `database_migration_dual_approval.sql` in a text editor, copy the contents, and paste into MySQL command line.

---

## ✅ VERIFICATION STEPS

After running the migration, verify it worked:

### **Check 1: Verify payroll table has new columns**

```sql
DESCRIBE payroll;
```

**Expected Output:**
```
+---------------------+-----------+------+-----+---------+----------------+
| Field               | Type      | Null | Key | Default | Extra          |
+---------------------+-----------+------+-----+---------+----------------+
| id                  | bigint    | NO   | PRI | NULL    | auto_increment |
| employee_id         | bigint    | YES  | MUL | NULL    |                |
| month               | int       | YES  |     | NULL    |                |
| year                | int       | YES  |     | NULL    |                |
| basic_salary        | double    | YES  |     | NULL    |                |
| ...                 | ...       | ...  | ... | ...     | ...            |
| requires_dual_approval | tinyint(1) | YES |     | 1       |             |
| accountant_approved    | tinyint(1) | YES |     | 0       |             |
| director_approved      | tinyint(1) | YES |     | 0       |             |
| final_approval_date    | date       | YES |     | NULL    |             |
| payslip_generated      | tinyint(1) | YES |     | 0       |             |
+---------------------+-----------+------+-----+---------+----------------+
```

---

### **Check 2: Verify payroll_approvals table exists**

```sql
SHOW TABLES LIKE 'payroll_approvals';
DESCRIBE payroll_approvals;
```

**Expected Output:**
```
+------------------+--------------+------+-----+---------------------+
| Field            | Type         | Null | Key | Default             |
+------------------+--------------+------+-----+---------------------+
| id               | bigint       | NO   | PRI | NULL                |
| payroll_id       | bigint       | NO   | MUL | NULL                |
| approver_role    | enum(...)    | NO   |     | NULL                |
| approved_by      | bigint       | YES  | MUL | NULL                |
| status           | enum(...)    | NO   |     | PENDING             |
| approved_at      | timestamp    | YES  |     | NULL                |
| rejection_reason | varchar(500) | YES  |     | NULL                |
| comments         | varchar(500) | YES  |     | NULL                |
| created_at       | timestamp    | YES  |     | CURRENT_TIMESTAMP   |
+------------------+--------------+------+-----+---------------------+
```

---

### **Check 3: View current payrolls with approval status**

```sql
SELECT 
    p.id AS payroll_id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    p.month,
    p.year,
    p.status AS payroll_status,
    p.accountant_approved,
    p.director_approved,
    p.payslip_generated,
    CASE 
        WHEN p.accountant_approved = TRUE AND p.director_approved = TRUE 
        THEN '📄 Ready'
        ELSE '⏳ Pending'
    END AS payslip_status
FROM payroll p
JOIN employees e ON p.employee_id = e.id
ORDER BY p.year DESC, p.month DESC
LIMIT 10;
```

---

## 🔧 TROUBLESHOOTING

### **Error: "Table 'payroll' doesn't exist"**
**Solution:** Make sure you're using the correct database:
```sql
USE hrmsystem;
```

### **Error: "Duplicate column name"**
**Cause:** Migration was already run  
**Solution:** This is OK! The columns already exist. Skip to verification.

### **Error: "Table 'payroll_approvals' already exists"**
**Cause:** Migration was already run  
**Solution:** Drop and recreate:
```sql
DROP TABLE IF EXISTS payroll_approvals;
-- Then run migration again
```

### **Error: "Access denied for user 'root'"**
**Cause:** Wrong password or permissions  
**Solution:** Use correct password or contact DBA

### **Error: MySQL command not found**
**Cause:** MySQL not in PATH  
**Solution:** Use full path to mysql.exe or use MySQL Workbench

---

## 🎯 QUICK FIX SCRIPT

If something goes wrong, here's a rollback script:

```sql
-- ROLLBACK SCRIPT
USE hrmsystem;

-- Remove new columns from payroll
ALTER TABLE payroll 
DROP COLUMN requires_dual_approval,
DROP COLUMN accountant_approved,
DROP COLUMN director_approved,
DROP COLUMN final_approval_date,
DROP COLUMN payslip_generated;

-- Drop approvals table
DROP TABLE IF EXISTS payroll_approvals;
```

Save as `rollback_dual_approval.sql` and run if needed.

---

## 📊 POST-MIGRATION CHECKLIST

After migration, verify:

- [ ] `payroll` table has 5 new columns
- [ ] `payroll_approvals` table created
- [ ] Existing payrolls have default values set
- [ ] No SQL errors during migration
- [ ] Database backup created (optional but recommended)

---

## 🚀 NEXT STEPS

After successful migration:

### **Step 1: Restart Spring Boot Application**
```bash
# Stop current instance (Ctrl+C)
# Then restart
mvn spring-boot:run
```

### **Step 2: Test API Endpoints**
```bash
# Test pending approvals endpoint
curl http://localhost:8080/api/payroll/approvals/pending/accountant

# Should return empty array or list of pending approvals
[]
```

### **Step 3: Create Test Payroll**
1. Login as HR
2. Create a new payroll for an employee
3. Check database: status should be `PENDING_APPROVAL`
4. Check `payroll_approvals` table: should have 2 records

---

## 📞 NEED HELP?

If you encounter issues:

1. Check MySQL error log
2. Verify hrmsystem database exists
3. Ensure you have proper permissions
4. Try Method 2 (MySQL Workbench) - most reliable

---

## ✅ SUCCESS INDICATORS

You'll know migration succeeded when:

✅ SQL script runs without errors  
✅ See message: `✅ Dual Approval Payroll Migration Complete!`  
✅ `DESCRIBE payroll` shows new columns  
✅ `SHOW TABLES` includes `payroll_approvals`  
✅ Application starts without database errors  

---

**Ready to proceed!** Choose Method 1 (MySQL Workbench) for easiest execution. 🚀
