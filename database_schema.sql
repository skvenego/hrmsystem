-- HRM System Complete Database Schema
-- Created: April 2026
-- Features: Dual Approval System, Leave Management, Payroll, Attendance

-- Drop existing tables if they exist (for fresh setup)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS notification_preferences;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS payslips;
DROP TABLE IF EXISTS payroll;
DROP TABLE IF EXISTS leave_balances;
DROP TABLE IF EXISTS leaves;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Departments Table
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    head_of_department VARCHAR(100),
    created_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_department_name (name)
);

-- 2. Employees Table
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    department_id BIGINT,
    designation VARCHAR(100),
    employee_code VARCHAR(20) UNIQUE,
    joining_date DATE NOT NULL,
    probation_end_date DATE,
    salary DECIMAL(15, 2),
    basic_salary DECIMAL(15, 2),
    da DECIMAL(15, 2) DEFAULT 0,
    hra DECIMAL(15, 2) DEFAULT 0,
    other_allowance DECIMAL(15, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, ON_LEAVE, TERMINATED',
    employment_type VARCHAR(20) DEFAULT 'PERMANENT' COMMENT 'PERMANENT, CONTRACT, PROBATION',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    INDEX idx_employee_email (email),
    INDEX idx_employee_code (employee_code),
    INDEX idx_employee_department (department_id),
    INDEX idx_joining_date (joining_date)
);

-- 3. Users Table (Authentication)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'EMPLOYEE' COMMENT 'EMPLOYEE, HR, ADMIN, DIRECTOR, ACCOUNTANT',
    employee_id BIGINT,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    department VARCHAR(100),
    position VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    password_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role),
    INDEX idx_employee_id (employee_id)
);

-- 4. Attendance Table
CREATE TABLE attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    working_hours DECIMAL(4, 2) DEFAULT 0,
    overtime_hours DECIMAL(4, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PRESENT' COMMENT 'PRESENT, ABSENT, LEAVE, HALF_DAY, HOLIDAY',
    remarks VARCHAR(255),
    late_arrival BOOLEAN DEFAULT FALSE,
    early_departure BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (employee_id, date),
    INDEX idx_attendance_employee_date (employee_id, date),
    INDEX idx_attendance_date (date),
    INDEX idx_attendance_status (status)
);

-- 5. Leave Table
CREATE TABLE leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL COMMENT 'SICK, CASUAL, ANNUAL, MATERNITY, PATERNITY',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(3, 1) NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, CANCELLED',
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    applied_date DATE NOT NULL,
    approved_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_leave_employee (employee_id),
    INDEX idx_leave_status (status),
    INDEX idx_leave_dates (start_date, end_date),
    INDEX idx_leave_type (leave_type)
);

-- 6. Leave Balance Table
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    sick_leave DECIMAL(3, 1) DEFAULT 12,
    casual_leave DECIMAL(3, 1) DEFAULT 12,
    annual_leave DECIMAL(3, 1) DEFAULT 15,
    maternity_leave DECIMAL(3, 1) DEFAULT 90,
    paternity_leave DECIMAL(3, 1) DEFAULT 15,
    earned_leave DECIMAL(3, 1) DEFAULT 0,
    balance_carried_forward DECIMAL(3, 1) DEFAULT 0,
    last_updated DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_leave_balance (employee_id, year),
    INDEX idx_leave_balance_employee (employee_id),
    INDEX idx_leave_balance_year (year)
);

-- 7. Payroll Table (with Dual Approval)
CREATE TABLE payroll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    
    -- Earnings
    basic_salary DECIMAL(15, 2),
    hra DECIMAL(15, 2) DEFAULT 0,
    da DECIMAL(15, 2) DEFAULT 0,
    ta DECIMAL(15, 2) DEFAULT 0,
    other_allowances DECIMAL(15, 2) DEFAULT 0,
    overtime_pay DECIMAL(15, 2) DEFAULT 0,
    gross_salary DECIMAL(15, 2),
    
    -- Deductions
    provident_fund DECIMAL(15, 2) DEFAULT 0,
    tax DECIMAL(15, 2) DEFAULT 0,
    insurance DECIMAL(15, 2) DEFAULT 0,
    professional_tax DECIMAL(15, 2) DEFAULT 0,
    other_deductions DECIMAL(15, 2) DEFAULT 0,
    absent_deduction DECIMAL(15, 2) DEFAULT 0,
    total_deductions DECIMAL(15, 2),
    
    -- Net Salary
    net_salary DECIMAL(15, 2),
    
    -- Attendance Details
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    half_days INT DEFAULT 0,
    working_days INT DEFAULT 26,
    
    -- Dual Approval System
    accountant_approval BOOLEAN DEFAULT FALSE,
    accountant_approved_by VARCHAR(100),
    accountant_approved_date TIMESTAMP NULL,
    director_approval BOOLEAN DEFAULT FALSE,
    director_approved_by VARCHAR(100),
    director_approved_date TIMESTAMP NULL,
    
    -- Status
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, PAID, REJECTED',
    payment_date DATE,
    payment_method VARCHAR(20) COMMENT 'BANK_TRANSFER, CASH, CHEQUE',
    transaction_id VARCHAR(100),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_payroll (employee_id, month, year),
    INDEX idx_payroll_employee (employee_id),
    INDEX idx_payroll_month_year (month, year),
    INDEX idx_payroll_status (status),
    INDEX idx_payroll_approvals (accountant_approval, director_approval)
);

-- 8. Payslips Table (Enhanced with Dual Approval)
CREATE TABLE payslips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    payroll_id BIGINT,
    month_year VARCHAR(7) NOT NULL,
    salary_month INT NOT NULL,
    salary_year INT NOT NULL,
    
    -- Earnings
    basic_salary DECIMAL(15, 2),
    da DECIMAL(15, 2) DEFAULT 0,
    hra DECIMAL(15, 2) DEFAULT 0,
    other_allowance DECIMAL(15, 2) DEFAULT 0,
    overtime_pay DECIMAL(15, 2) DEFAULT 0,
    gross_salary DECIMAL(15, 2),
    
    -- Deductions
    pf DECIMAL(15, 2) DEFAULT 0,
    esi DECIMAL(15, 2) DEFAULT 0,
    income_tax DECIMAL(15, 2) DEFAULT 0,
    professional_tax DECIMAL(15, 2) DEFAULT 0,
    other_deduction DECIMAL(15, 2) DEFAULT 0,
    absent_leave_deduction DECIMAL(15, 2) DEFAULT 0,
    total_deduction DECIMAL(15, 2),
    
    -- Net Salary
    net_salary DECIMAL(15, 2),
    
    -- Attendance Details
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    half_days INT DEFAULT 0,
    working_days INT DEFAULT 26,
    total_days INT DEFAULT 30,
    
    -- Status & Approvals
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT, PENDING, APPROVED, PAID',
    payroll_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, PAID',
    accountant_approval BOOLEAN DEFAULT FALSE,
    accountant_approved_by VARCHAR(100),
    accountant_approved_date TIMESTAMP NULL,
    director_approval BOOLEAN DEFAULT FALSE,
    director_approved_by VARCHAR(100),
    director_approved_date TIMESTAMP NULL,
    
    -- PDF Generation
    pdf_generated BOOLEAN DEFAULT FALSE,
    pdf_file_path VARCHAR(500),
    pdf_generated_date TIMESTAMP NULL,
    
    -- Email
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date TIMESTAMP NULL,
    
    -- Audit
    generated_date DATE,
    approved_by VARCHAR(100),
    approved_date DATE,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE SET NULL,
    UNIQUE KEY unique_payslip (employee_id, month_year),
    INDEX idx_payslip_employee (employee_id),
    INDEX idx_payslip_month_year (month_year),
    INDEX idx_payslip_status (status),
    INDEX idx_payslip_approvals (accountant_approval, director_approval),
    INDEX idx_payslip_payroll_status (payroll_status)
);

-- 9. Notifications Table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'GENERAL' COMMENT 'GENERAL, APPROVAL, ALERT, LEAVE, PAYROLL',
    priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, URGENT',
    is_read BOOLEAN DEFAULT FALSE,
    action_required BOOLEAN DEFAULT FALSE,
    action_url VARCHAR(500),
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_notification_employee (employee_id),
    INDEX idx_notification_type (type),
    INDEX idx_notification_read (is_read),
    INDEX idx_notification_created (created_at)
);

-- 10. Notification Preferences Table
CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    leave_notifications BOOLEAN DEFAULT TRUE,
    payroll_notifications BOOLEAN DEFAULT TRUE,
    attendance_notifications BOOLEAN DEFAULT TRUE,
    general_notifications BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_notification_preferences (employee_id)
);

-- Insert Default Departments
INSERT INTO departments (name, description, head_of_department, created_date) VALUES
('Human Resources', 'Manages employee relations, recruitment, and policies', 'HR Manager', CURDATE()),
('Finance', 'Handles financial planning, accounting, and payroll', 'Finance Manager', CURDATE()),
('Information Technology', 'Manages IT infrastructure and software development', 'IT Manager', CURDATE()),
('Sales', 'Handles sales, marketing, and customer relations', 'Sales Manager', CURDATE()),
('Operations', 'Manages daily operations and logistics', 'Operations Manager', CURDATE()),
('Administration', 'Handles administrative tasks and facilities', 'Admin Manager', CURDATE());

-- Create Default Admin User (Password: admin123)
INSERT INTO users (username, email, password, role, first_name, last_name, is_active) VALUES
('admin', 'admin@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'System', 'Administrator', TRUE);

-- Create Test Users for Different Roles
INSERT INTO users (username, email, password, role, first_name, last_name, is_active) VALUES
('hr', 'hr@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'HR', 'HR', 'Manager', TRUE),
('accountant', 'accountant@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ACCOUNTANT', 'Accountant', 'User', TRUE),
('director', 'director@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DIRECTOR', 'Director', 'User', TRUE),
('sachin', 'sachin@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'EMPLOYEE', 'Sachin', 'Employee', TRUE);

-- Create Sample Employees
INSERT INTO employees (first_name, last_name, email, phone, address, department_id, designation, employee_code, joining_date, probation_end_date, salary, basic_salary, status, employment_type) VALUES
('Sachin', 'Employee', 'sachin@hrm.com', '+1234567890', '123 Main Street, City', 1, 'Software Engineer', 'EMP001', '2024-01-15', '2024-04-15', 50000.00, 30000.00, 'ACTIVE', 'PERMANENT'),
('John', 'Doe', 'john.doe@hrm.com', '+1234567891', '456 Oak Avenue, City', 2, 'Accountant', 'EMP002', '2024-02-01', '2024-05-01', 45000.00, 27000.00, 'ACTIVE', 'PERMANENT'),
('Jane', 'Smith', 'jane.smith@hrm.com', '+1234567892', '789 Pine Road, City', 3, 'HR Manager', 'EMP003', '2023-06-15', '2023-09-15', 60000.00, 36000.00, 'ACTIVE', 'PERMANENT'),
('Mike', 'Wilson', 'mike.wilson@hrm.com', '+1234567893', '321 Elm Street, City', 4, 'Sales Executive', 'EMP004', '2024-03-01', '2024-06-01', 35000.00, 21000.00, 'ACTIVE', 'PROBATION'),
('Sarah', 'Johnson', 'sarah.johnson@hrm.com', '+1234567894', '654 Maple Drive, City', 5, 'Operations Manager', 'EMP005', '2023-12-01', '2024-03-01', 55000.00, 33000.00, 'ACTIVE', 'PERMANENT');

-- Update user-employee relationships
UPDATE users SET employee_id = 1 WHERE username = 'sachin';
UPDATE users SET employee_id = 2 WHERE username = 'john';
UPDATE users SET employee_id = 3 WHERE username = 'jane';
UPDATE users SET employee_id = 4 WHERE username = 'mike';
UPDATE users SET employee_id = 5 WHERE username = 'sarah';

-- Initialize Leave Balances for Current Year
INSERT INTO leave_balances (employee_id, year, sick_leave, casual_leave, annual_leave, maternity_leave, paternity_leave, earned_leave, last_updated) VALUES
(1, YEAR(CURDATE()), 12, 12, 15, 90, 15, 18, CURDATE()),
(2, YEAR(CURDATE()), 12, 12, 15, 90, 15, 15, CURDATE()),
(3, YEAR(CURDATE()), 12, 12, 15, 90, 15, 27, CURDATE()),
(4, YEAR(CURDATE()), 12, 12, 15, 90, 15, 3, CURDATE()),
(5, YEAR(CURDATE()), 12, 12, 15, 90, 15, 21, CURDATE());

-- Initialize Notification Preferences
INSERT INTO notification_preferences (employee_id) VALUES
(1), (2), (3), (4), (5);

-- Create Sample Attendance Records (Current Month)
INSERT INTO attendance (employee_id, date, check_in_time, check_out_time, working_hours, status, remarks) VALUES
(1, CURDATE(), '09:00:00', '18:00:00', 8.5, 'PRESENT', 'Regular attendance'),
(2, CURDATE(), '08:45:00', '17:30:00', 8.25, 'PRESENT', 'Regular attendance'),
(3, CURDATE(), '09:15:00', '18:15:00', 8.5, 'PRESENT', 'Late arrival'),
(4, CURDATE(), NULL, NULL, 0, 'ABSENT', 'Absent without information'),
(5, CURDATE(), '08:30:00', '17:00:00', 8.0, 'PRESENT', 'Early departure');

-- Create Sample Leave Requests
INSERT INTO leaves (employee_id, leave_type, start_date, end_date, total_days, reason, status, applied_date) VALUES
(1, 'CASUAL', DATE_ADD(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY), 2, 'Personal work', 'PENDING', CURDATE()),
(2, 'SICK', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), 2, 'Fever and cold', 'APPROVED', DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
(3, 'ANNUAL', DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 18 DAY), 5, 'Family vacation', 'PENDING', CURDATE());

-- Create Sample Payroll Records (Current Month)
INSERT INTO payroll (
    employee_id, month, year, basic_salary, hra, da, gross_salary, 
    provident_fund, tax, total_deductions, net_salary,
    present_days, absent_days, leave_days, working_days,
    status, created_at
) VALUES
(1, MONTH(CURDATE()), YEAR(CURDATE()), 30000.00, 9000.00, 3000.00, 42000.00, 3600.00, 2000.00, 5600.00, 36400.00, 22, 0, 0, 22, 'PENDING', CURDATE()),
(2, MONTH(CURDATE()), YEAR(CURDATE()), 27000.00, 8100.00, 2700.00, 37800.00, 3240.00, 1500.00, 4740.00, 33060.00, 22, 0, 0, 22, 'PENDING', CURDATE()),
(3, MONTH(CURDATE()), YEAR(CURDATE()), 36000.00, 10800.00, 3600.00, 50400.00, 4320.00, 3000.00, 7320.00, 43080.00, 22, 0, 0, 22, 'PENDING', CURDATE());

-- Create Sample Payslips
INSERT INTO payslips (
    employee_id, payroll_id, month_year, salary_month, salary_year,
    basic_salary, hra, da, gross_salary, pf, income_tax, total_deduction, net_salary,
    present_days, absent_days, leave_days, half_days, working_days, total_days,
    status, payroll_status, accountant_approval, director_approval,
    generated_date, created_at
) VALUES
(1, 1, CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0')), MONTH(CURDATE()), YEAR(CURDATE()),
 30000.00, 9000.00, 3000.00, 42000.00, 3600.00, 2000.00, 5600.00, 36400.00,
 22, 0, 0, 0, 22, 30, 'DRAFT', 'PENDING', FALSE, FALSE, CURDATE(), CURDATE()),
(2, 2, CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0')), MONTH(CURDATE()), YEAR(CURDATE()),
 27000.00, 8100.00, 2700.00, 37800.00, 3240.00, 1500.00, 4740.00, 33060.00,
 22, 0, 0, 0, 22, 30, 'DRAFT', 'PENDING', FALSE, FALSE, CURDATE(), CURDATE()),
(3, 3, CONCAT(YEAR(CURDATE()), '-', LPAD(MONTH(CURDATE()), 2, '0')), MONTH(CURDATE()), YEAR(CURDATE()),
 36000.00, 10800.00, 3600.00, 50400.00, 4320.00, 3000.00, 7320.00, 43080.00,
 22, 0, 0, 0, 22, 30, 'DRAFT', 'PENDING', FALSE, FALSE, CURDATE(), CURDATE());

-- Create Sample Notifications
INSERT INTO notifications (employee_id, title, message, type, priority, action_required, action_url) VALUES
(1, 'Leave Application Submitted', 'Your casual leave request for 2 days has been submitted and is pending approval.', 'LEAVE', 'MEDIUM', TRUE, '/dashboard/leave'),
(2, 'Leave Approved', 'Your sick leave request has been approved by HR.', 'APPROVAL', 'HIGH', FALSE, '/dashboard/leave'),
(3, 'Payroll Processing', 'Your payroll for this month is being processed. Payslips will be available after approval.', 'PAYROLL', 'MEDIUM', FALSE, '/dashboard/payslips'),
(4, 'Attendance Marked', 'Your attendance for today has been marked as absent. Please contact HR if this is incorrect.', 'ALERT', 'HIGH', TRUE, '/dashboard/attendance'),
(5, 'System Update', 'HRM system has been updated with new features. Check your dashboard for latest updates.', 'GENERAL', 'LOW', FALSE, '/dashboard');

-- Create Triggers for Automatic Updates

DELIMITER //

-- Trigger to update leave balance when leave is approved
CREATE TRIGGER update_leave_balance_after_approval
AFTER UPDATE ON leaves
FOR EACH ROW
BEGIN
    IF NEW.status = 'APPROVED' AND OLD.status != 'APPROVED' THEN
        UPDATE leave_balances 
        SET 
            CASE NEW.leave_type
                WHEN 'SICK' THEN sick_leave = sick_leave - NEW.total_days
                WHEN 'CASUAL' THEN casual_leave = casual_leave - NEW.total_days
                WHEN 'ANNUAL' THEN annual_leave = annual_leave - NEW.total_days
                WHEN 'MATERNITY' THEN maternity_leave = maternity_leave - NEW.total_days
                WHEN 'PATERNITY' THEN paternity_leave = paternity_leave - NEW.total_days
            END,
            last_updated = CURDATE()
        WHERE employee_id = NEW.employee_id AND year = YEAR(NEW.start_date);
    END IF;
END//

-- Trigger to create notification when leave is applied
CREATE TRIGGER notify_leave_application
AFTER INSERT ON leaves
FOR EACH ROW
BEGIN
    INSERT INTO notifications (employee_id, title, message, type, priority, action_required, action_url)
    VALUES (
        NEW.employee_id,
        'Leave Application Submitted',
        CONCAT('Your ', NEW.leave_type, ' leave request for ', NEW.total_days, ' days has been submitted.'),
        'LEAVE',
        'MEDIUM',
        TRUE,
        '/dashboard/leave'
    );
END//

-- Trigger to create notification when leave is approved/rejected
CREATE TRIGGER notify_leave_status_change
AFTER UPDATE ON leaves
FOR EACH ROW
BEGIN
    IF NEW.status = 'APPROVED' AND OLD.status != 'APPROVED' THEN
        INSERT INTO notifications (employee_id, title, message, type, priority, action_required)
        VALUES (
            NEW.employee_id,
            'Leave Approved',
            CONCAT('Your ', NEW.leave_type, ' leave request has been approved.'),
            'APPROVAL',
            'HIGH',
            FALSE
        );
    ELSEIF NEW.status = 'REJECTED' AND OLD.status != 'REJECTED' THEN
        INSERT INTO notifications (employee_id, title, message, type, priority, action_required)
        VALUES (
            NEW.employee_id,
            'Leave Rejected',
            CONCAT('Your ', NEW.leave_type, ' leave request has been rejected. Reason: ', COALESCE(NEW.rejection_reason, 'Not specified')),
            'ALERT',
            'HIGH',
            FALSE
        );
    END IF;
END//

DELIMITER ;

-- Create Views for Common Queries

CREATE VIEW employee_details AS
SELECT 
    e.id,
    e.first_name,
    e.last_name,
    e.email,
    e.phone,
    e.address,
    e.designation,
    e.employee_code,
    e.joining_date,
    e.probation_end_date,
    e.salary,
    e.status,
    e.employment_type,
    d.name as department_name,
    u.username,
    u.role as user_role,
    TIMESTAMPDIFF(MONTH, e.joining_date, CURDATE()) as months_worked,
    CASE 
        WHEN e.probation_end_date > CURDATE() THEN 'PROBATION'
        ELSE 'ACTIVE'
    END as probation_status
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
LEFT JOIN users u ON e.id = u.employee_id;

CREATE VIEW monthly_attendance_summary AS
SELECT 
    e.id as employee_id,
    e.first_name,
    e.last_name,
    e.employee_code,
    MONTH(a.date) as month,
    YEAR(a.date) as year,
    COUNT(CASE WHEN a.status = 'PRESENT' THEN 1 END) as present_days,
    COUNT(CASE WHEN a.status = 'ABSENT' THEN 1 END) as absent_days,
    COUNT(CASE WHEN a.status = 'LEAVE' THEN 1 END) as leave_days,
    COUNT(CASE WHEN a.status = 'HALF_DAY' THEN 1 END) as half_days,
    SUM(a.working_hours) as total_hours,
    SUM(a.overtime_hours) as total_overtime
FROM employees e
LEFT JOIN attendance a ON e.id = a.employee_id
WHERE YEAR(a.date) = YEAR(CURDATE())
GROUP BY e.id, MONTH(a.date), YEAR(a.date);

CREATE VIEW payroll_summary AS
SELECT 
    e.id as employee_id,
    e.first_name,
    e.last_name,
    e.employee_code,
    p.month,
    p.year,
    p.basic_salary,
    p.gross_salary,
    p.total_deductions,
    p.net_salary,
    p.status,
    p.accountant_approval,
    p.director_approval,
    p.payment_date,
    CASE 
        WHEN p.accountant_approval = TRUE AND p.director_approval = TRUE THEN 'FULLY_APPROVED'
        WHEN p.accountant_approval = TRUE OR p.director_approval = TRUE THEN 'PARTIALLY_APPROVED'
        ELSE 'PENDING'
    END as approval_status
FROM employees e
LEFT JOIN payroll p ON e.id = p.employee_id
WHERE p.year = YEAR(CURDATE()) AND p.month = MONTH(CURDATE());

-- Final Setup
-- Update database version info
INSERT INTO departments (name, description) VALUES 
('System', 'System configuration and metadata')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Display setup completion message
SELECT 'HRM Database Schema Created Successfully!' as message,
       COUNT(*) as total_tables_created
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name IN ('departments', 'employees', 'users', 'attendance', 'leaves', 'leave_balances', 'payroll', 'payslips', 'notifications', 'notification_preferences');

-- Show sample data counts
SELECT 'Sample Data Created' as info,
       (SELECT COUNT(*) FROM employees) as employees,
       (SELECT COUNT(*) FROM users) as users,
       (SELECT COUNT(*) FROM departments) as departments,
       (SELECT COUNT(*) FROM attendance) as attendance_records,
       (SELECT COUNT(*) FROM leaves) as leave_requests,
       (SELECT COUNT(*) FROM payroll) as payroll_records,
       (SELECT COUNT(*) FROM payslips) as payslips,
       (SELECT COUNT(*) FROM notifications) as notifications;
