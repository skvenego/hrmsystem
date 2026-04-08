# HRM System Database Setup Guide

## 🗄️ Database Schema Overview

This HRM system uses MySQL with a comprehensive schema supporting:
- **Dual Approval System** for payroll and payslips
- **Leave Management** with balance tracking and probation periods
- **Attendance Tracking** with detailed time records
- **Employee Management** with role-based access
- **Notification System** for real-time updates

## 📋 Tables Overview

### Core Tables

1. **departments** - Company departments
2. **employees** - Employee information and profiles
3. **users** - Authentication and user roles
4. **attendance** - Daily attendance records
5. **leaves** - Leave requests and approvals
6. **leave_balances** - Leave balance tracking
7. **payroll** - Monthly payroll with dual approval
8. **payslips** - Generated payslips with approval status
9. **notifications** - Employee notifications
10. **notification_preferences** - Notification settings

## 🚀 Quick Setup

### Option 1: Quick Setup (Recommended for development)
```bash
mysql -u root -p < quick_setup.sql
```

### Option 2: Complete Setup (Production ready)
```bash
mysql -u root -p < database_schema.sql
```

## 🔧 Default Credentials

| Username | Email | Password | Role |
|----------|-------|----------|------|
| admin | admin@hrm.com | admin123 | ADMIN |
| hr | hr@hrm.com | admin123 | HR |
| accountant | accountant@hrm.com | admin123 | ACCOUNTANT |
| director | director@hrm.com | admin123 | DIRECTOR |
| sachin | sachin@hrm.com | admin123 | EMPLOYEE |

## 🏗️ Key Features

### Dual Approval System
- **Accountant Approval**: First level approval for payroll
- **Director Approval**: Final approval for payment release
- **Status Tracking**: PENDING → PARTIALLY_APPROVED → FULLY_APPROVED → PAID

### Leave Management
- **Probation Period**: 3 months before leave eligibility
- **Leave Accrual**: 1.5 leaves per month after probation
- **Leave Types**: SICK, CASUAL, ANNUAL, MATERNITY, PATERNITY
- **Expiry Cycles**: Jan-Jul and Jul-Dec

### Employee Roles
- **EMPLOYEE**: Can view dashboard, apply leave, download approved payslips
- **HR**: Manage employees, attendance, leave approvals
- **ACCOUNTANT**: Process payroll, approve payslips (first level)
- **DIRECTOR**: Final approval for payroll, overall management
- **ADMIN**: Full system access

## 📊 Sample Data

The setup includes:
- **5 Departments**: HR, Finance, IT, Sales, Operations
- **3 Sample Employees**: Different roles and departments
- **Leave Balances**: Pre-configured for current year
- **Attendance Records**: Current month data
- **Payroll Records**: Current month pending approval
- **Notifications**: Sample notifications for testing

## 🔄 Database Views

### employee_details
Complete employee information with department and role details.

### monthly_attendance_summary
Monthly attendance statistics per employee.

### payroll_summary
Payroll status with approval tracking.

## ⚡ Triggers

### Automatic Leave Balance Update
Updates leave balance when leave is approved.

### Notification Generation
Creates notifications for:
- Leave applications
- Leave approvals/rejections
- Payroll status changes

## 🔍 Indexes for Performance

- Employee email and code lookups
- Attendance date queries
- Leave status tracking
- Payroll month/year searches
- Notification filtering

## 📝 Database Schema Relationships

```
departments (1) ←→ (N) employees
employees (1) ←→ (1) users
employees (1) ←→ (N) attendance
employees (1) ←→ (N) leaves
employees (1) ←→ (N) leave_balances
employees (1) ←→ (N) payroll
employees (1) ←→ (N) payslips
employees (1) ←→ (N) notifications
payroll (1) ←→ (1) payslips
```

## 🛠️ Maintenance

### Monthly Tasks
1. Process payroll for all employees
2. Update leave balances
3. Generate payslips
4. Archive old attendance records

### Yearly Tasks
1. Reset leave balances
2. Archive previous year data
3. Update tax calculations
4. Backup database

## 🔐 Security Notes

- All passwords are hashed using BCrypt
- Role-based access control implemented
- Foreign key constraints ensure data integrity
- Sensitive data (salaries) protected by role permissions

## 📈 Scalability Considerations

- Partitioning by year for large datasets
- Index optimization for frequent queries
- Archive old data to maintain performance
- Regular backups and maintenance

## 🐛 Troubleshooting

### Common Issues

1. **Foreign Key Errors**: Ensure data is inserted in correct order
2. **Duplicate Entry Errors**: Check unique constraints
3. **Permission Issues**: Verify MySQL user permissions

### Reset Database
```sql
DROP DATABASE IF EXISTS hrm_system;
CREATE DATABASE hrm_system;
```

## 📞 Support

For database-related issues:
1. Check MySQL error logs
2. Verify connection settings in application.properties
3. Ensure proper user permissions
4. Review foreign key constraints

---

**Database Version**: 1.0  
**Last Updated**: April 2026  
**Compatible with**: MySQL 8.0+
