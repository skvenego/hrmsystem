-- ============================================================
-- PROFESSIONAL HRMS SYSTEM - DATABASE MIGRATION
-- ============================================================
-- This script adds advanced features:
-- 1. Probation Period Tracking
-- 2. Enhanced Leave Management with Accrual
-- 3. Leave Transaction History
-- ============================================================

-- Step 1: Create Probation Periods Table
CREATE TABLE IF NOT EXISTS probation_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT UNIQUE NOT NULL,
    start_date DATE NOT NULL COMMENT 'Probation start date',
    end_date DATE NOT NULL COMMENT 'Probation end date',
    duration_months INT NOT NULL DEFAULT 6 COMMENT 'Duration in months',
    status ENUM('PROBATION', 'CONFIRMED', 'EXTENDED') NOT NULL DEFAULT 'PROBATION',
    paid_leave_eligible BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Eligible for paid leaves after probation',
    pf_percentage DOUBLE NOT NULL DEFAULT 12.0 COMMENT 'Provident Fund percentage',
    annual_tax DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Annual tax amount',
    monthly_tds DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Monthly TDS (auto-calculated)',
    extension_notes VARCHAR(500) COMMENT 'Notes if probation extended',
    confirmed_by VARCHAR(100) COMMENT 'HR/Manager who confirmed',
    confirmed_date DATE COMMENT 'Confirmation date',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_employee_status (employee_id, status),
    INDEX idx_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 2: Create Enhanced Leave Balances Table
CREATE TABLE IF NOT EXISTS leave_balances_enhanced (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL COMMENT 'Leave year',
    earned_leaves DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Leaves earned in year',
    total_available DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Total available (earned + carried)',
    used_leaves DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Leaves used',
    remaining_leaves DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Remaining balance',
    carried_forward DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Carried from previous year',
    lapsed_leaves DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Lapsed/expired leaves',
    last_updated DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_employee_year (employee_id, year),
    INDEX idx_employee_year (employee_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 3: Create Leave Transactions Table
CREATE TABLE IF NOT EXISTS leave_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    leave_balance_id BIGINT NOT NULL,
    leave_id BIGINT UNIQUE COMMENT 'Reference to actual leave application',
    transaction_type ENUM('EARNED', 'USED', 'CANCELLED', 'CARRIED_FORWARD', 'LAPSED') NOT NULL,
    days DOUBLE NOT NULL DEFAULT 0.0,
    balance_after DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Balance after this transaction',
    transaction_date DATE DEFAULT CURRENT_DATE,
    notes VARCHAR(500) COMMENT 'Transaction notes/reason',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (leave_balance_id) REFERENCES leave_balances_enhanced(id) ON DELETE CASCADE,
    FOREIGN KEY (leave_id) REFERENCES leaves(id) ON DELETE SET NULL,
    INDEX idx_balance_date (leave_balance_id, transaction_date),
    INDEX idx_transaction_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Step 4: Update User Roles to Support New Roles
-- Add new roles if not already present
ALTER TABLE users MODIFY COLUMN role ENUM(
    'ROLE_BM', 
    'ROLE_HR', 
    'ROLE_ACCOUNTANT', 
    'ROLE_DIRECTOR', 
    'ROLE_EMPLOYEE',
    'ROLE_ADMIN'
) NOT NULL DEFAULT 'ROLE_EMPLOYEE';

-- Step 5: Initialize Probation Periods for Existing Employees
INSERT INTO probation_periods (employee_id, start_date, duration_months, status, paid_leave_eligible, pf_percentage, annual_tax, monthly_tds)
SELECT 
    e.id,
    COALESCE(e.joining_date, CURDATE()) as start_date,
    6 as duration_months,
    CASE 
        WHEN DATEDIFF(CURDATE(), COALESCE(e.joining_date, CURDATE())) > 180 THEN 'CONFIRMED'
        ELSE 'PROBATION'
    END as status,
    CASE 
        WHEN DATEDIFF(CURDATE(), COALESCE(e.joining_date, CURDATE())) > 180 THEN TRUE
        ELSE FALSE
    END as paid_leave_eligible,
    12.0 as pf_percentage,
    COALESCE(
        (SELECT p.annual_tax FROM payroll p WHERE p.employee_id = e.id LIMIT 1),
        0.0
    ) as annual_tax,
    ROUND(COALESCE(
        (SELECT p.annual_tax FROM payroll p WHERE p.employee_id = e.id LIMIT 1),
        0.0
    ) / 12, 2) as monthly_tds
FROM employees e
WHERE e.id NOT IN (
    SELECT employee_id FROM probation_periods WHERE employee_id IS NOT NULL
);

-- Step 6: Initialize Enhanced Leave Balances for Current Year
INSERT INTO leave_balances_enhanced (employee_id, year, earned_leaves, total_available, used_leaves, remaining_leaves, carried_forward)
SELECT 
    e.id,
    YEAR(CURDATE()) as year,
    0.0 as earned_leaves,
    0.0 as total_available,
    0.0 as used_leaves,
    0.0 as remaining_leaves,
    0.0 as carried_forward
FROM employees e
WHERE e.id NOT IN (
    SELECT employee_id FROM leave_balances_enhanced 
    WHERE year = YEAR(CURDATE()) AND employee_id IS NOT NULL
);

-- Step 7: Create View for Employee Probation Status
CREATE OR REPLACE VIEW v_employee_probation AS
SELECT 
    e.id as employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    e.email,
    e.department,
    e.designation,
    pp.status as probation_status,
    pp.start_date,
    pp.end_date,
    pp.duration_months,
    pp.paid_leave_eligible,
    pp.pf_percentage,
    pp.annual_tax,
    pp.monthly_tds,
    pp.confirmed_by,
    pp.confirmed_date,
    DATEDIFF(pp.end_date, CURDATE()) as days_remaining,
    CASE 
        WHEN pp.status = 'CONFIRMED' THEN 'Confirmed Employee'
        WHEN DATEDIFF(pp.end_date, CURDATE()) < 0 THEN 'Probation Overdue'
        WHEN DATEDIFF(pp.end_date, CURDATE()) <= 15 THEN 'Confirmation Due Soon'
        ELSE 'In Probation'
    END as status_description
FROM employees e
LEFT JOIN probation_periods pp ON e.id = pp.employee_id
WHERE e.status != 'TERMINATED';

-- Step 8: Create View for Leave Balance Summary
CREATE OR REPLACE VIEW v_leave_balance_summary AS
SELECT 
    e.id as employee_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    e.department,
    lbe.year,
    lbe.earned_leaves,
    lbe.total_available,
    lbe.used_leaves,
    lbe.remaining_leaves,
    lbe.carried_forward,
    lbe.lapsed_leaves,
    lbe.last_updated,
    pp.status as probation_status,
    pp.paid_leave_eligible,
    CASE 
        WHEN pp.paid_leave_eligible = FALSE THEN 'Not Eligible (Probation)'
        WHEN lbe.remaining_leaves <= 0 THEN 'No Balance'
        WHEN lbe.remaining_leaves <= 5 THEN 'Low Balance'
        ELSE 'Available'
    END as leave_status
FROM employees e
LEFT JOIN leave_balances_enhanced lbe ON e.id = lbe.employee_id AND lbe.year = YEAR(CURDATE())
LEFT JOIN probation_periods pp ON e.id = pp.employee_id
WHERE e.status != 'TERMINATED';

-- Step 9: Create View for Leave Transactions Detail
CREATE OR REPLACE VIEW v_leave_transactions_detail AS
SELECT 
    lt.id as transaction_id,
    CONCAT(e.first_name, ' ', e.last_name) as employee_name,
    e.department,
    lbe.year,
    lt.transaction_type,
    lt.days,
    lt.balance_after,
    lt.transaction_date,
    lt.notes,
    l.leave_type,
    l.start_date,
    l.end_date,
    l.reason
FROM leave_transactions lt
JOIN leave_balances_enhanced lbe ON lt.leave_balance_id = lbe.id
JOIN employees e ON lbe.employee_id = e.id
LEFT JOIN leaves l ON lt.leave_id = l.id
ORDER BY lt.transaction_date DESC, lt.id DESC;

-- Step 10: Display Migration Summary
SELECT 'Migration Completed Successfully!' as status;

SELECT 'PROBATION PERIODS' as category, COUNT(*) as records FROM probation_periods
UNION ALL
SELECT 'LEAVE BALANCES', COUNT(*) FROM leave_balances_enhanced WHERE year = YEAR(CURDATE())
UNION ALL
SELECT 'LEAVE TRANSACTIONS', COUNT(*) FROM leave_transactions
UNION ALL
SELECT 'TOTAL EMPLOYEES', COUNT(*) FROM employees
UNION ALL
SELECT 'EMPLOYEES IN PROBATION', COUNT(*) FROM probation_periods WHERE status = 'PROBATION'
UNION ALL
SELECT 'CONFIRMED EMPLOYEES', COUNT(*) FROM probation_periods WHERE status = 'CONFIRMED';

-- Display sample data
SELECT * FROM v_employee_probation LIMIT 10;
SELECT * FROM v_leave_balance_summary LIMIT 10;
