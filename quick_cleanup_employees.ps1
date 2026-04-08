# ============================================
# Quick Employee Data Cleanup Script
# ============================================
# This script removes orphaned records and verifies employee deletion
# ============================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  EMPLOYEE DATA CLEANUP SCRIPT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Database credentials
$dbName = "hrm_db"
$user = "root"
$password = "Sachin@123"

# Function to run MySQL query
function Run-MySQLQuery {
    param($query)
    
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo.FileName = "mysql"
    $process.StartInfo.Arguments = "-u $user -p$password $dbName -e `"$query`""
    $process.StartInfo.UseShellExecute = $false
    $process.StartInfo.RedirectStandardOutput = $true
    $process.StartInfo.RedirectStandardError = $true
    $process.Start()
    
    $output = $process.StandardOutput.ReadToEnd()
    $errorOutput = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    
    if ($errorOutput) {
        Write-Host "Error: $errorOutput" -ForegroundColor Red
        return $null
    }
    
    return $output
}

# Step 1: Check current employees
Write-Host "`n[Step 1] Current Employees in Database:" -ForegroundColor Yellow
Run-MySQLQuery "SELECT id, first_name, last_name, email, status FROM employees ORDER BY id;"

# Step 2: Check for orphaned records
Write-Host "`n[Step 2] Checking for Orphaned Records..." -ForegroundColor Yellow

$orphanPayroll = Run-MySQLQuery "SELECT COUNT(*) as count FROM payroll WHERE employee_id IS NULL;"
$orphanAttendance = Run-MySQLQuery "SELECT COUNT(*) as count FROM attendance WHERE employee_id IS NULL;"
$orphanLeaves = Run-MySQLQuery "SELECT COUNT(*) as count FROM leaves WHERE employee_id IS NULL;"
$orphanPayslips = Run-MySQLQuery "SELECT COUNT(*) as count FROM payslips WHERE employee_id IS NULL;"

Write-Host "Orphan Payroll: $orphanPayroll"
Write-Host "Orphan Attendance: $orphanAttendance"
Write-Host "Orphan Leaves: $orphanLeaves"
Write-Host "Orphan Payslips: $orphanPayslips"

# Step 3: Ask if user wants to clean up
Write-Host ""
$response = Read-Host "Do you want to delete all orphaned records? (y/n)"

if ($response -eq 'y' -or $response -eq 'Y') {
    Write-Host "`n[Step 3] Deleting Orphaned Records..." -ForegroundColor Yellow
    
    Write-Host "  - Deleting orphan payroll records..."
    Run-MySQLQuery "DELETE FROM payroll WHERE employee_id IS NULL;"
    
    Write-Host "  - Deleting orphan attendance records..."
    Run-MySQLQuery "DELETE FROM attendance WHERE employee_id IS NULL;"
    
    Write-Host "  - Deleting orphan leave records..."
    Run-MySQLQuery "DELETE FROM leaves WHERE employee_id IS NULL;"
    
    Write-Host "  - Deleting orphan payslip records..."
    Run-MySQLQuery "DELETE FROM payslips WHERE employee_id IS NULL;"
    
    Write-Host "`n✅ Orphaned records deleted successfully!" -ForegroundColor Green
} else {
    Write-Host "`n⏭️  Skipped cleanup." -ForegroundColor Yellow
}

# Step 4: Show record counts
Write-Host "`n[Step 4] Current Record Counts:" -ForegroundColor Yellow
Run-MySQLQuery "
SELECT 'employees' as table_name, COUNT(*) as count FROM employees
UNION ALL SELECT 'payroll', COUNT(*) FROM payroll
UNION ALL SELECT 'attendance', COUNT(*) FROM attendance
UNION ALL SELECT 'leaves', COUNT(*) FROM leaves
UNION ALL SELECT 'payslips', COUNT(*) FROM payslips;
"

# Step 5: Instructions
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  CLEANUP COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Refresh your browser: Ctrl + Shift + R" -ForegroundColor White
Write-Host "2. Check Payroll page - orphaned records should be gone" -ForegroundColor White
Write-Host "3. To delete a specific employee:" -ForegroundColor White
Write-Host "   - Go to Employees page" -ForegroundColor White
Write-Host "   - Click Delete button (🗑️)" -ForegroundColor White
Write-Host "   - Confirm deletion" -ForegroundColor White
Write-Host ""
Write-Host "If Rahul still appears:" -ForegroundColor Red
Write-Host "- Clear browser cache completely" -ForegroundColor White
Write-Host "- Try incognito/private mode" -ForegroundColor White
Write-Host "- Run: check_employee_full_data.sql in MySQL" -ForegroundColor White
Write-Host ""
