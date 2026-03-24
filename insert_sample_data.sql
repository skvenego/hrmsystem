-- Insert sample employees if not exists
INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, other_allowance, status, address)
SELECT 'John', 'Doe', 'john.doe@example.com', '1234567890', 
       (SELECT id FROM departments WHERE name = 'IT' LIMIT 1), 
       'Software Engineer', '2024-01-15', 75000.00, 
       50000.00, 10000.00, 15000.00, 5000.00, 'ACTIVE', '123 Main St, City'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE email = 'john.doe@example.com');

INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, other_allowance, status, address)
SELECT 'Jane', 'Smith', 'jane.smith@example.com', '0987654321',
       (SELECT id FROM departments WHERE name = 'HR' LIMIT 1),
       'HR Manager', '2023-06-20', 85000.00,
       60000.00, 12000.00, 18000.00, 6000.00, 'ACTIVE', '456 Oak Ave, City'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE email = 'jane.smith@example.com');

INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, other_allowance, status, address)
SELECT 'Mike', 'Johnson', 'mike.johnson@example.com', '1122334455',
       (SELECT id FROM departments WHERE name = 'Sales' LIMIT 1),
       'Sales Executive', '2024-03-10', 65000.00,
       45000.00, 9000.00, 13500.00, 4500.00, 'ACTIVE', '789 Pine Rd, City'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE email = 'mike.johnson@example.com');

INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, other_allowance, status, address)
SELECT 'Sarah', 'Williams', 'sarah.williams@example.com', '5566778899',
       (SELECT id FROM departments WHERE name = 'Design' LIMIT 1),
       'UI/UX Designer', '2024-02-05', 70000.00,
       48000.00, 9600.00, 14400.00, 4800.00, 'ACTIVE', '321 Elm St, City'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE email = 'sarah.williams@example.com');

INSERT INTO employees (first_name, last_name, email, phone, department_id, designation, joining_date, salary, basic_salary, da, hra, other_allowance, status, address)
SELECT 'David', 'Brown', 'david.brown@example.com', '9988776655',
       (SELECT id FROM departments WHERE name = 'Finance' LIMIT 1),
       'Accountant', '2023-11-12', 80000.00,
       55000.00, 11000.00, 16500.00, 5500.00, 'ACTIVE', '654 Maple Dr, City'
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE email = 'david.brown@example.com');
