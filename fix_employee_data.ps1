# Quick Fix Script for Employee Data Not Showing
# Run this script to insert sample employee data

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Employee Data Fix Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Get MySQL credentials
$mysqlUser = Read-Host "Enter MySQL username (default: root)"
if ([string]::IsNullOrWhiteSpace($mysqlUser)) {
    $mysqlUser = "root"
}

$mysqlPassword = Read-Host "Enter MySQL password" -AsSecureString
$plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($mysqlPassword)
)

$database = Read-Host "Enter database name (default: hrmsystem)"
if ([string]::IsNullOrWhiteSpace($database)) {
    $database = "hrmsystem"
}

Write-Host ""
Write-Host "Inserting sample employee data..." -ForegroundColor Yellow

# SQL commands
$sql = @"
-- Insert Departments if not exist
INSERT INTO departments (name, description, head_of_department, created_date) VALUES
('Information Technology', 'IT and Software Development', 'John Smith', CURDATE())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO departments (name, description, head_of_department, created_date) VALUES
('Human Resources', 'HR and Recruitment', 'Sarah Johnson', CURDATE())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO departments (name, description, head_of_department, created_date) VALUES
('Finance', 'Accounting and Finance', 'Michael Brown', CURDATE())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO departments (name, description, head_of_department, created_date) VALUES
('Marketing', 'Marketing and Sales', 'Emily Davis', CURDATE())
ON DUPLICATE KEY UPDATE name=name;

-- Insert Sample Employees
INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, status, address) VALUES
('John', 'Doe', 'john.doe@company.com', '+1234567890', 1, 'Software Engineer', '2024-01-15', 75000.00, 50000.00, 15000.00, 10000.00, 'ACTIVE', '123 Main Street, City'),
('Jane', 'Smith', 'jane.smith@company.com', '+0987654321', 2, 'HR Manager', '2023-06-01', 85000.00, 55000.00, 18000.00, 12000.00, 'ACTIVE', '456 Oak Avenue, Town'),
('Bob', 'Wilson', 'bob.wilson@company.com', '+1122334455', 1, 'Senior Developer', '2022-03-20', 95000.00, 65000.00, 20000.00, 10000.00, 'ACTIVE', '789 Pine Road, Village'),
('Alice', 'Brown', 'alice.brown@company.com', '+5544332211', 3, 'Accountant', '2023-09-10', 65000.00, 45000.00, 12000.00, 8000.00, 'ACTIVE', '321 Elm Street, City'),
('Charlie', 'Davis', 'charlie.davis@company.com', '+9988776655', 4, 'Marketing Executive', '2024-02-01', 70000.00, 48000.00, 14000.00, 8000.00, 'ACTIVE', '654 Maple Drive, Town'),
('Eva', 'Martinez', 'eva.martinez@company.com', '+7766554433', 1, 'Full Stack Developer', '2023-11-15', 88000.00, 60000.00, 18000.00, 10000.00, 'ACTIVE', '987 Cedar Lane, City'),
('Frank', 'Garcia', 'frank.garcia@company.com', '+4433221100', 1, 'DevOps Engineer', '2023-04-20', 92000.00, 62000.00, 20000.00, 10000.00, 'ACTIVE', '147 Birch Street, Village'),
('Grace', 'Rodriguez', 'grace.rodriguez@company.com', '+2211009988', 2, 'Recruiter', '2024-01-05', 58000.00, 40000.00, 10000.00, 8000.00, 'ACTIVE', '258 Spruce Avenue, Town');

-- Verify
SELECT 
    e.id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
    e.email,
    e.phone,
    d.name AS department,
    e.designation,
    e.salary,
    e.status
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id
ORDER BY e.id;
"@

# Execute SQL
try {
    $env:MYSQL_PWD = $plainPassword
    $result = mysql -u $mysqlUser $database -e $sql 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ SUCCESS!" -ForegroundColor Green
        Write-Host "Sample employee data inserted successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Employees added: 8" -ForegroundColor Cyan
        Write-Host "Departments added: 4" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor Yellow
        Write-Host "1. Restart your Spring Boot application" -ForegroundColor White
        Write-Host "2. Navigate to: http://localhost:8080/employees" -ForegroundColor White
        Write-Host "3. You should see all 8 employees!" -ForegroundColor White
    } else {
        throw "MySQL command failed"
    }
}
catch {
    Write-Host ""
    Write-Host "✗ ERROR!" -ForegroundColor Red
    Write-Host "Failed to insert employee data." -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible solutions:" -ForegroundColor Yellow
    Write-Host "1. Make sure MySQL is running" -ForegroundColor White
    Write-Host "2. Check database name is correct" -ForegroundColor White
    Write-Host "3. Verify credentials are correct" -ForegroundColor White
    Write-Host "4. Try running insert_sample_employees.sql manually" -ForegroundColor White
}
finally {
    $env:MYSQL_PWD = $null
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
