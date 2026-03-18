-- Create Payslip Table
CREATE TABLE payslips (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    month_year VARCHAR(7) NOT NULL,
    
    -- Attendance
    total_days INT DEFAULT 30,
    present_days INT DEFAULT 0,
    absent_days INT DEFAULT 0,
    leave_days INT DEFAULT 0,
    
    -- Earnings
    basic_salary DECIMAL(10, 2),
    da DECIMAL(10, 2) DEFAULT 0,
    hra DECIMAL(10, 2) DEFAULT 0,
    allowances DECIMAL(10, 2) DEFAULT 0,
    gross_salary DECIMAL(10, 2),
    
    -- Deductions
    pf DECIMAL(10, 2) DEFAULT 0,
    esi DECIMAL(10, 2) DEFAULT 0,
    income_tax DECIMAL(10, 2) DEFAULT 0,
    other_deductions DECIMAL(10, 2) DEFAULT 0,
    total_deduction DECIMAL(10, 2),
    
    -- Net Salary
    net_salary DECIMAL(10, 2),
    
    -- Status & Approval
    status VARCHAR(20) DEFAULT 'DRAFT',
    approved_by VARCHAR(255),
    approved_date DATETIME,
    remarks TEXT,
    
    -- PDF
    pdf_file_path VARCHAR(500),
    pdf_generated_date DATETIME,
    
    -- Email
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_date DATETIME,
    
    -- Audit
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    UNIQUE KEY unique_payslip (employee_id, month_year)
);

-- Create Indexes for Performance
CREATE INDEX idx_employee_month ON payslips(employee_id, month_year);
CREATE INDEX idx_status ON payslips(status);
CREATE INDEX idx_created_date ON payslips(created_date);
CREATE INDEX idx_month_year ON payslips(month_year);
CREATE INDEX idx_email_sent ON payslips(email_sent);