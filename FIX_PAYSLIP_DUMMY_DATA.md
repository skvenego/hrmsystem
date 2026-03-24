# Fix Dummy Payslip Data - Regenerate All Payslips

## Problem
Your payslips table shows dummy data with all zeros because:
1. Payslips were generated BEFORE employee salary data was added
2. The existing payslip records have stale/placeholder data
3. Need to regenerate payslips to pull real salary data from employees table

## Solution: Regenerate All Payslips

### Option 1: Delete Old Payslips and Regenerate (RECOMMENDED)

Run these SQL commands to clear old payslips:

```sql
-- Backup existing payslips (optional)
CREATE TABLE payslips_backup AS SELECT * FROM payslips;

-- Delete all GENERATED payslips (keeps APPROVED and SENT ones)
DELETE FROM payslips WHERE status = 'GENERATED';

-- Verify deletion
SELECT COUNT(*) FROM payslips;
```

Then regenerate via API (see Option 3 below).

### Option 2: Update Existing Payslips Directly

If you want to keep the same payslip IDs, update them directly:

```sql
-- Update payslips with actual employee salary data
UPDATE payslips p
INNER JOIN employees e ON p.employee_id = e.id
SET 
    p.basic_salary = COALESCE(e.basic_salary, 0),
    p.da = COALESCE(e.da, 0),
    p.hra = COALESCE(e.hra, 0),
    p.other_allowance = COALESCE(e.other_allowance, 0),
    p.gross_salary = COALESCE(e.basic_salary, 0) + COALESCE(e.da, 0) + COALESCE(e.hra, 0) + COALESCE(e.other_allowance, 0),
    p.pf = COALESCE(e.basic_salary, 0) * 0.12,
    p.esi = 0,
    p.income_tax = 0,
    p.total_deduction = COALESCE(e.basic_salary, 0) * 0.12,
    p.net_salary = (COALESCE(e.basic_salary, 0) + COALESCE(e.da, 0) + COALESCE(e.hra, 0) + COALESCE(e.other_allowance, 0)) - (COALESCE(e.basic_salary, 0) * 0.12)
WHERE p.status = 'GENERATED';
```

### Option 3: Regenerate via API (EASIEST)

Use Postman or your frontend to call these endpoints:

#### Step 1: Check Employee Data First
```bash
GET http://localhost:8080/api/employees
```

Verify employees have salary data (basic_salary, da, hra, other_allowance should NOT be 0).

#### Step 2: Delete Old Generated Payslips
```bash
# Get all payslips first
GET http://localhost:8080/api/payslips

# Note down the IDs of GENERATED payslips you want to delete
# Then delete them one by one (only DRAFT can be deleted via API)
# OR run the SQL DELETE command from Option 1 above
```

#### Step 3: Regenerate Payslips for Current Month (March 2026)
```bash
# Generate for specific employee
POST http://localhost:8080/api/payslips/generate?employeeId=2&monthYear=2026-03

# Or bulk generate for ALL employees
POST http://localhost:8080/api/payslips/bulk-generate?monthYear=2026-03
```

#### Step 4: Verify New Data
```bash
GET http://localhost:8080/api/payslips
```

You should now see REAL salary values instead of zeros!

## Expected Result

After regeneration, your payslip should show:

| Field | Example Value for Employee with ₹75,000 salary |
|-------|-----------------------------------------------|
| basic_salary | 50000.00 |
| da | 10000.00 |
| hra | 15000.00 |
| other_allowance | 5000.00 |
| gross_salary | 80000.00 |
| pf | 6000.00 (12% of basic) |
| total_deduction | 6000.00 |
| net_salary | 74000.00 |

## Quick Fix Script

Save this as `fix_payslips.sql` and run it:

```sql
USE hrm_db;

-- Show current payslips
SELECT id, employee_id, month_year, basic_salary, gross_salary, net_salary, status 
FROM payslips 
ORDER BY id;

-- Delete GENERATED payslips (they will be regenerated)
DELETE FROM payslips WHERE status = 'GENERATED';

-- Verify
SELECT COUNT(*) as remaining_payslips FROM payslips;

-- Show employee salary data (this is what should appear in payslips)
SELECT id, CONCAT(first_name, ' ', last_name) as name, email, 
       basic_salary, da, hra, other_allowance, salary
FROM employees;
```

Run it with:
```bash
mysql -u root -pSachin@123 hrm_db < fix_payslips.sql
```

Then regenerate via the frontend or API.

## Why This Happened

1. Payslips were generated when employees had NULL or zero salary fields
2. The system saved those zero values to the payslips table
3. Even after updating employee salaries, the old payslip records remained unchanged
4. Need to either UPDATE or DELETE+REGENERATE the payslips

## Prevention

Always ensure employees have correct salary data BEFORE generating payslips:
1. Add/Edit employee with complete salary details
2. THEN generate payslip
3. The system will automatically calculate correct values
