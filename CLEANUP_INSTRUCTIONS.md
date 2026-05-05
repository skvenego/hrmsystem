# Project Cleanup Instructions

## Files to Move to `archive/` folder (keep for reference):

### SQL Debug Files (can be archived)
- check_april_data.sql
- check_april_leaves.sql
- check_data_fixed.sql
- check_leave_balance_data.sql
- check_leave_dates.sql
- check_may_payroll.sql
- check_payroll_data.sql
- check_schema.sql
- debug_april_error.sql
- debug_leave_balance.sql
- debug_leaves.sql
- fix_april_payroll.sql
- fix_data_corrected.sql
- fix_data_final.sql
- fix_employee16_payroll.sql
- fix_employee_leaves.sql
- fix_leave_data.sql
- fix_multi_month_leave.sql
- fix_pending_leave.sql
- show_and_fix_leaves.sql
- show_april_data.sql
- test_may_calculation.sql
- verify_leave_fix.sql

### Documentation Files (can be archived)
- data_source_analysis.md
- FIELD_NAME_FIXES.md
- LEAVE_CALCULATION_RULES.md (superseded by ATTENDANCE_LEAVE_CALCULATION_RULES.md)

## Files to DELETE (dangerous/one-time use):
- clear_all_data.sql - Destructive script
- cleanup_duplicates.sql - One-time use
- delete_payslip_cascade.sql - One-time use
- diagnose_and_fix.sql - One-time use
- find_employee.sql - Debug query
- migration_add_modified_status.sql - Already applied
- migration_update_attendance_status.sql - Already applied
- query.sql - Generic temp query

## Files to KEEP:
- database_schema.sql - Master database schema
- PROJECT_DOCUMENTATION.md - Main project documentation
- ATTENDANCE_LEAVE_CALCULATION_RULES.md - Current calculation rules
- ALL_FIXES_SUMMARY.md - History of changes
