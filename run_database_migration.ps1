# Dual Approval Payroll Database Migration Script
# This script runs the SQL migration for dual-approval payroll workflow

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DUAL APPROVAL PAYROLL DATABASE MIGRATION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$mysqlPath = Read-Host "Enter MySQL path (default: C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe)"
if ([string]::IsNullOrWhiteSpace($mysqlPath)) {
    $mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
}

$password = Read-Host "Enter MySQL root password" -AsSecureString
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
)

$sqlFile = "c:\Users\GCV\Projects\hrmsystem\database_migration_dual_approval.sql"
$database = "hrmsystem"

Write-Host ""
Write-Host "Running migration..." -ForegroundColor Yellow
Write-Host ""

try {
    # Run the SQL file
    & $mysqlPath -u root -p"$plainPassword" $database < $sqlFile
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "MIGRATION COMPLETED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Changes made:" -ForegroundColor Cyan
    Write-Host "✓ Added columns to payroll table" -ForegroundColor Green
    Write-Host "✓ Created payroll_approvals table" -ForegroundColor Green
    Write-Host "✓ Initialized existing payrolls" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next step: Restart your Spring Boot application" -ForegroundColor Yellow
} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "ERROR: Migration failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error details: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "1. MySQL is running" -ForegroundColor White
    Write-Host "2. Password is correct" -ForegroundColor White
    Write-Host "3. hrmsystem database exists" -ForegroundColor White
}
