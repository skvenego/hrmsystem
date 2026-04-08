-- Clear and Reset Employee Tables for HRM Database
-- Run this in MySQL to clear all employee data and reset structure

-- Disable foreign key checks
SET FOREIGN_KEY_CHECKS = 0;

-- Drop and recreate employee-related tables in correct order
DROP TABLE IF EXISTS payslips;
DROP TABLE IF EXISTS payrolls;
DROP TABLE IF EXISTS leave_balances;
DROP TABLE IF EXISTS attendances;
DROP TABLE IF EXISTS leaves;
DROP TABLE IF EXISTS employees;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Recreate employees table with all fields matching the form
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    department_id BIGINT,
    designation VARCHAR(100) NOT NULL,
    joining_date DATE NOT NULL,
    salary DECIMAL(12,2),
    basic_salary DECIMAL(12,2),
    da DECIMAL(12,2),
    hra DECIMAL(12,2),
    other_allowance DECIMAL(12,2),
    status ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE', 'TERMINATED') DEFAULT 'ACTIVE',
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_department (department_id),
    INDEX idx_status (status)
);

-- Recreate related tables (simplified structure)
CREATE TABLE leaves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE attendances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in TIME,
    check_out TIME,
    status ENUM('PRESENT', 'ABSENT', 'HALF_DAY') DEFAULT 'PRESENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(50) NOT NULL,
    total_days DECIMAL(5,1) DEFAULT 0,
    used_days DECIMAL(5,1) DEFAULT 0,
    balance_days DECIMAL(5,1) DEFAULT 0,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE payrolls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month VARCHAR(20) NOT NULL,
    year INT NOT NULL,
    basic_salary DECIMAL(12,2),
    da DECIMAL(12,2),
    hra DECIMAL(12,2),
    other_allowance DECIMAL(12,2),
    gross_salary DECIMAL(12,2),
    pf_deduction DECIMAL(12,2) DEFAULT 0,
    tax_deduction DECIMAL(12,2) DEFAULT 0,
    net_salary DECIMAL(12,2),
    payment_date DATE,
    status ENUM('PENDING', 'PAID') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

CREATE TABLE payslips (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    month VARCHAR(20) NOT NULL,
    year INT NOT NULL,
    file_path VARCHAR(500),
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- Insert sample departments
INSERT IGNORE INTO departments (id, name) VALUES 
(1, 'Information Technology'),
(2, 'Human Resources'),
(3, 'Finance'),
(4, 'Operations'),
(5, 'Sales'),
(6, 'Marketing');

-- Reset auto-increment counters
ALTER TABLE employees AUTO_INCREMENT = 1;
ALTER TABLE leaves AUTO_INCREMENT = 1;
ALTER TABLE attendances AUTO_INCREMENT = 1;
ALTER TABLE leave_balances AUTO_INCREMENT = 1;
ALTER TABLE payrolls AUTO_INCREMENT = 1;
ALTER TABLE payslips AUTO_INCREMENT = 1;

-- Show table structures
DESCRIBE employees;
DESCRIBE leaves;
DESCRIBE attendances;
DESCRIBE leave_balances;
DESCRIBE payrolls;
DESCRIBE payslips;

SELECT 'Employee tables cleared and recreated successfully!' as status;
