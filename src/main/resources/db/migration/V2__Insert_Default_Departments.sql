-- Insert default departments (ignore if already exists)
INSERT IGNORE INTO departments (name, description, head_of_department, created_date) VALUES
('IT', 'Information Technology Department', NULL, CURDATE()),
('HR', 'Human Resources Department', NULL, CURDATE()),
('Sales', 'Sales and Marketing Department', NULL, CURDATE()),
('Design', 'Design and Creative Department', NULL, CURDATE()),
('Finance', 'Finance and Accounting Department', NULL, CURDATE()),
('Marketing', 'Marketing Department', NULL, CURDATE()),
('Product', 'Product Management Department', NULL, CURDATE()),
('Operations', 'Operations Department', NULL, CURDATE());
