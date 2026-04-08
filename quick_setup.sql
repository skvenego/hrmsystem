-- HRM System Quick Database Setup
-- Run this script for initial database setup

-- Create Database (if not exists)
CREATE DATABASE IF NOT EXISTS hrm_system 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE hrm_system;

-- Key Tables Setup
SET FOREIGN_KEY_CHECKS = 0;

-- Departments
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    head_of_department VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Employees
CREATE TABLE IF NOT EXISTS employees (
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
    status VARCHAR(20) DEFAULT 'ACTIVE',
    employment_type VARCHAR(20) DEFAULT 'PERMANENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'EMPLOYEE',
    employee_id BIGINT,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL
);

-- Attendance
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    date DATE NOT NULL,
    check_in_time TIME,
    check_out_time TIME,
    working_hours DECIMAL(4, 2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PRESENT',
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (employee_id, date)
);

-- Leaves
CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days DECIMAL(3, 1) NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    applied_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- Leave Balances
CREATE TABLE IF NOT EXISTS leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year INT NOT NULL,
    sick_leave DECIMAL(3, 1) DEFAULT 12,
    casual_leave DECIMAL(3, 1) DEFAULT 12,
    annual_leave DECIMAL(3, 1) DEFAULT 15,
    earned_leave DECIMAL(3, 1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_leave_balance (employee_id, year)
);

-- Payroll (with Dual Approval)
CREATE TABLE IF NOT EXISTS payroll (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    basic_salary DECIMAL(15, 2),
    hra DECIMAL(15, 2) DEFAULT 0,
    da DECIMAL(15, 2) DEFAULT 0,
    gross_salary DECIMAL(15, 2),
    provident_fund DECIMAL(15, 2) DEFAULT 0,
    tax DECIMAL(15, 2) DEFAULT 0,
    total_deductions DECIMAL(15, 2),
    net_salary DECIMAL(15, 2),
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    working_days INT DEFAULT 26,
    accountant_approval BOOLEAN DEFAULT FALSE,
    accountant_approved_by VARCHAR(100),
    accountant_approved_date TIMESTAMP NULL,
    director_approval BOOLEAN DEFAULT FALSE,
    director_approved_by VARCHAR(100),
    director_approved_date TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    payment_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_payroll (employee_id, month, year)
);

-- Payslips (with Dual Approval)
CREATE TABLE IF NOT EXISTS payslips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    payroll_id BIGINT,
    month_year VARCHAR(7) NOT NULL,
    salary_month INT NOT NULL,
    salary_year INT NOT NULL,
    basic_salary DECIMAL(15, 2),
    hra DECIMAL(15, 2) DEFAULT 0,
    da DECIMAL(15, 2) DEFAULT 0,
    gross_salary DECIMAL(15, 2),
    pf DECIMAL(15, 2) DEFAULT 0,
    income_tax DECIMAL(15, 2) DEFAULT 0,
    total_deduction DECIMAL(15, 2),
    net_salary DECIMAL(15, 2),
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    half_days INT DEFAULT 0,
    working_days INT DEFAULT 26,
    status VARCHAR(20) DEFAULT 'DRAFT',
    payroll_status VARCHAR(20) DEFAULT 'PENDING',
    accountant_approval BOOLEAN DEFAULT FALSE,
    accountant_approved_by VARCHAR(100),
    accountant_approved_date TIMESTAMP NULL,
    director_approval BOOLEAN DEFAULT FALSE,
    director_approved_by VARCHAR(100),
    director_approved_date TIMESTAMP NULL,
    pdf_generated BOOLEAN DEFAULT FALSE,
    pdf_file_path VARCHAR(500),
    email_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE SET NULL,
    UNIQUE KEY unique_payslip (employee_id, month_year)
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'GENERAL',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

SET FOREIGN_KEY_CHECKS = 1;

-- Basic Data Insertion
INSERT IGNORE INTO departments (name, description) VALUES
('Human Resources', 'HR Department'),
('Finance', 'Finance Department'),
('IT', 'Information Technology'),
('Sales', 'Sales Department'),
('Operations', 'Operations Department');

-- Default Admin User (Password: admin123)
INSERT IGNORE INTO users (username, email, password, role, first_name, last_name, is_active) VALUES
('admin', 'admin@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', 'System', 'Administrator', TRUE);

-- Test Users
INSERT IGNORE INTO users (username, email, password, role, first_name, last_name, is_active) VALUES
('hr', 'hr@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'HR', 'HR', 'Manager', TRUE),
('accountant', 'accountant@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ACCOUNTANT', 'Accountant', 'User', TRUE),
('director', 'director@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'DIRECTOR', 'Director', 'User', TRUE),
('sachin', 'sachin@hrm.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'EMPLOYEE', 'Sachin', 'Employee', TRUE);

-- Sample Employees
INSERT IGNORE INTO employees (first_name, last_name, email, phone, department_id, designation, employee_code, joining_date, salary, basic_salary, status) VALUES
('Sachin', 'Employee', 'sachin@hrm.com', '+1234567890', 1, 'Software Engineer', 'EMP001', '2024-01-15', 50000.00, 30000.00, 'ACTIVE'),
('John', 'Doe', 'john.doe@hrm.com', '+1234567891', 2, 'Accountant', 'EMP002', '2024-02-01', 45000.00, 27000.00, 'ACTIVE'),
('Jane', 'Smith', 'jane.smith@hrm.com', '+1234567892', 3, 'HR Manager', 'EMP003', '2023-06-15', 60000.00, 36000.00, 'ACTIVE');

-- Update user-employee relationships
UPDATE users SET employee_id = (SELECT id FROM employees WHERE email = users.email) WHERE email IN ('sachin@hrm.com', 'john.doe@hrm.com', 'jane.smith@hrm.com');

-- Initialize Leave Balances
INSERT IGNORE INTO leave_balances (employee_id, year, sick_leave, casual_leave, annual_leave, earned_leave) VALUES
((SELECT id FROM employees WHERE email = 'sachin@hrm.com'), YEAR(CURDATE()), 12, 12, 15, 18),
((SELECT id FROM employees WHERE email = 'john.doe@hrm.com'), YEAR(CURDATE()), 12, 12, 15, 15),
((SELECT id FROM employees WHERE email = 'jane.smith@hrm.com'), YEAR(CURDATE()), 12, 12, 15, 27);

SELECT 'HRM Database Setup Completed Successfully!' as status;
