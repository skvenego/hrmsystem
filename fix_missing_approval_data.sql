-- ============================================
-- FIX MISSING APPROVAL DATA FOR EXISTING PAYROLLS
-- Run this AFTER database_migration_dual_approval.sql
-- ============================================

USE hrm_db;

-- ============================================
-- STEP 1: Create approval records for ALL existing payrolls
-- ============================================

-- Create Accountant approval records
INSERT INTO payroll_approvals (payroll_id, approver_role, status, approved_at, comments)
SELECT 
    p.id,
    'ROLE_ACCOUNTANT',
    CASE 
        WHEN p.status = 'PAID' THEN 'APPROVED' 
        WHEN p.status = 'PROCESSED' THEN 'APPROVED'
        ELSE 'PENDING' 
    END,
    CASE 
        WHEN p.status IN ('PAID', 'PROCESSED') THEN NOW() 
        ELSE NULL 
    END,
    'Migrated from existing payroll system'
FROM payroll p
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_approvals pa 
    WHERE pa.payroll_id = p.id AND pa.approver_role = 'ROLE_ACCOUNTANT'
);

-- Create Director approval records
INSERT INTO payroll_approvals (payroll_id, approver_role, status, approved_at, comments)
SELECT 
    p.id,
    'ROLE_DIRECTOR',
    CASE 
        WHEN p.status = 'PAID' THEN 'APPROVED' 
        WHEN p.status = 'PROCESSED' THEN 'APPROVED'
        ELSE 'PENDING' 
    END,
    CASE 
        WHEN p.status IN ('PAID', 'PROCESSED') THEN NOW() 
        ELSE NULL 
    END,
    'Migrated from existing payroll system'
FROM payroll p
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_approvals pa 
    WHERE pa.payroll_id = p.id AND pa.approver_role = 'ROLE_DIRECTOR'
);

-- ============================================
-- STEP 2: Mark already-PAID payrolls as fully approved
-- ============================================

UPDATE payroll p
SET p.accountant_approved = TRUE,
    p.director_approved = TRUE,
    p.final_approval_date = COALESCE(p.payment_date, CURDATE()),
    p.payslip_generated = TRUE
WHERE p.status = 'PAID' 
AND p.payslip_generated = FALSE;

-- Also mark PROCESSED payrolls (they were partially approved before)
UPDATE payroll p
SET p.accountant_approved = TRUE,
    p.director_approved = TRUE,
    p.final_approval_date = COALESCE(p.payment_date, CURDATE())
WHERE p.status = 'PROCESSED' 
AND p.accountant_approved = FALSE 
AND p.director_approved = FALSE;

-- ============================================
-- STEP 3: Verification Queries
-- ============================================

SELECT '=== APPROVAL RECORDS CREATED ===' AS info;

SELECT 
    COUNT(*) AS total_payrolls,
    COUNT(CASE WHEN accountant_approved THEN 1 END) AS accountant_approved_count,
    COUNT(CASE WHEN director_approved THEN 1 END) AS director_approved_count,
    COUNT(CASE WHEN payslip_generated THEN 1 END) AS payslip_ready_count
FROM payroll;

SELECT '';
SELECT '=== APPROVAL TABLE STATS ===' AS info;

SELECT 
    approver_role,
    status,
    COUNT(*) AS count
FROM payroll_approvals
GROUP BY approver_role, status;

SELECT '';
SELECT '=== PAYROLL STATUS OVERVIEW ===' AS info;

SELECT 
    p.id AS payroll_id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    p.month,
    p.year,
    p.status AS payroll_status,
    CASE 
        WHEN p.accountant_approved = TRUE THEN '✅'
        WHEN p.accountant_approved = FALSE THEN '⏳'
    END AS accountant_status,
    CASE 
        WHEN p.director_approved = TRUE THEN '✅'
        WHEN p.director_approved = FALSE THEN '⏳'
    END AS director_status,
    CASE 
        WHEN p.payslip_generated = TRUE THEN '📄 Ready'
        ELSE '❌ Not Ready'
    END AS payslip_status
FROM payroll p
JOIN employees e ON p.employee_id = e.id
ORDER BY p.year DESC, p.month DESC;

SELECT '';
SELECT '=== PENDING APPROVALS (Ready for Workflow) ===' AS info;

SELECT 
    pa.id AS approval_id,
    pa.payroll_id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    p.month,
    p.year,
    p.net_salary,
    pa.approver_role,
    pa.status,
    pa.created_at
FROM payroll_approvals pa
JOIN payroll p ON pa.payroll_id = p.id
JOIN employees e ON p.employee_id = e.id
WHERE pa.status = 'PENDING'
ORDER BY p.year DESC, p.month DESC, pa.approver_role;

SELECT '';
SELECT '=== MIGRATION COMPLETE ===' AS status;
SELECT 'You can now restart your Spring Boot application' AS next_step;
