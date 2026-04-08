-- ============================================
-- DUAL APPROVAL PAYROLL WORKFLOW - DATABASE MIGRATION
-- ============================================
-- This script adds dual-approval functionality to payroll system
-- Requires BOTH Accountant AND Director approval before payslip is generated
-- ============================================

USE hrm_db;

-- Step 1: Add approval tracking columns to payroll table
ALTER TABLE payroll 
ADD COLUMN requires_dual_approval BOOLEAN DEFAULT TRUE COMMENT 'Does this payroll require dual approval?',
ADD COLUMN accountant_approved BOOLEAN DEFAULT FALSE COMMENT 'Has Accountant approved?',
ADD COLUMN director_approved BOOLEAN DEFAULT FALSE COMMENT 'Has Director approved?',
ADD COLUMN final_approval_date DATE COMMENT 'Date when both approvals completed',
ADD COLUMN payslip_generated BOOLEAN DEFAULT FALSE COMMENT 'Is payslip ready for employee to view?';

-- Step 2: Update payroll status enum to include PENDING_APPROVAL
-- Note: MySQL doesn't support ENUM modification easily, so we'll use string comparison
-- The Java enum already has: PENDING, PROCESSED, PENDING_APPROVAL, PAID

-- Step 3: Create payroll_approvals table to track individual approvals
CREATE TABLE IF NOT EXISTS payroll_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Approval record ID',
    payroll_id BIGINT NOT NULL COMMENT 'Reference to payroll.id',
    approver_role ENUM('ROLE_ACCOUNTANT', 'ROLE_DIRECTOR') NOT NULL COMMENT 'Which role must approve',
    approved_by BIGINT COMMENT 'User ID who approved (references users.id)',
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT 'Current approval status',
    approved_at TIMESTAMP NULL COMMENT 'When approval was given',
    rejection_reason VARCHAR(500) COMMENT 'Reason if rejected',
    comments VARCHAR(500) COMMENT 'Comments from approver',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
    
    -- Foreign keys
    CONSTRAINT fk_payroll_approvals_payroll 
        FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE,
    CONSTRAINT fk_payroll_approvals_user 
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_payroll_status (payroll_id, status),
    INDEX idx_approver_role (approver_role),
    INDEX idx_approved_by (approved_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tracks dual-approval workflow for payroll';

-- Step 4: Initialize existing payrolls with default values
UPDATE payroll 
SET requires_dual_approval = TRUE,
    accountant_approved = FALSE,
    director_approved = FALSE,
    payslip_generated = CASE 
        WHEN status = 'PAID' THEN TRUE 
        ELSE FALSE 
    END
WHERE requires_dual_approval IS NULL;

-- Step 5: Create sample approval records for existing paid payrolls
-- (This is optional - you may want to start fresh)
INSERT INTO payroll_approvals (payroll_id, approver_role, status, approved_at, comments)
SELECT 
    p.id,
    'ROLE_ACCOUNTANT',
    CASE WHEN p.status = 'PAID' THEN 'APPROVED' ELSE 'PENDING' END,
    CASE WHEN p.status = 'PAID' THEN NOW() ELSE NULL END,
    'Migrated from existing payroll'
FROM payroll p
WHERE p.status = 'PAID'
AND NOT EXISTS (
    SELECT 1 FROM payroll_approvals pa 
    WHERE pa.payroll_id = p.id AND pa.approver_role = 'ROLE_ACCOUNTANT'
);

INSERT INTO payroll_approvals (payroll_id, approver_role, status, approved_at, comments)
SELECT 
    p.id,
    'ROLE_DIRECTOR',
    CASE WHEN p.status = 'PAID' THEN 'APPROVED' ELSE 'PENDING' END,
    CASE WHEN p.status = 'PAID' THEN NOW() ELSE NULL END,
    'Migrated from existing payroll'
FROM payroll p
WHERE p.status = 'PAID'
AND NOT EXISTS (
    SELECT 1 FROM payroll_approvals pa 
    WHERE pa.payroll_id = p.id AND pa.approver_role = 'ROLE_DIRECTOR'
);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check payroll table structure
SELECT 'Payroll table columns:' AS info;
DESCRIBE payroll;

-- Check payroll_approvals table structure
SELECT 'Payroll Approvals table structure:' AS info;
DESCRIBE payroll_approvals;

-- View all pending approvals
SELECT 
    pa.id AS approval_id,
    pa.payroll_id,
    e.first_name,
    e.last_name,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    p.month,
    p.year,
    p.net_salary,
    pa.approver_role,
    pa.status,
    pa.comments
FROM payroll_approvals pa
JOIN payroll p ON pa.payroll_id = p.id
JOIN employees e ON p.employee_id = e.id
WHERE pa.status = 'PENDING'
ORDER BY p.year DESC, p.month DESC, pa.approver_role;

-- View fully approved payrolls (ready for payslip)
SELECT 
    p.id AS payroll_id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    p.month,
    p.year,
    p.net_salary,
    p.accountant_approved,
    p.director_approved,
    p.payslip_generated,
    p.final_approval_date
FROM payroll p
JOIN employees e ON p.employee_id = e.id
WHERE p.accountant_approved = TRUE 
AND p.director_approved = TRUE
AND p.payslip_generated = TRUE
ORDER BY p.final_approval_date DESC;

-- View approval workflow status for all payrolls
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

-- ============================================
-- ROLLBACK SCRIPT (IF NEEDED)
-- ============================================
/*
-- To rollback these changes, run:

ALTER TABLE payroll 
DROP COLUMN requires_dual_approval,
DROP COLUMN accountant_approved,
DROP COLUMN director_approved,
DROP COLUMN final_approval_date,
DROP COLUMN payslip_generated;

DROP TABLE IF EXISTS payroll_approvals;
*/

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
SELECT '✅ Dual Approval Payroll Migration Complete!' AS status;
SELECT 'Run this next: Restart your Spring Boot application' AS next_step;
