# PowerShell Script to Fix Dummy Payslip Data
# This will delete old generated payslips so they can be regenerated with real data

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "FIX DUMMY PAYSLIP DATA" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$password = "Sachin@123"
$database = "hrm_db"
$username = "root"

# Check if MySQL is available
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    $mysqlCommand = "mysql"
} elseif (Test-Path $mysqlPath) {
    $mysqlCommand = $mysqlPath
} else {
    Write-Host "ERROR: MySQL not found!" -ForegroundColor Red
    Write-Host "Please install MySQL or add it to PATH" -ForegroundColor Yellow
    exit 1
}

Write-Host "Step 1: Showing current payslips..." -ForegroundColor Green
& $mysqlCommand -u $username -p$password $database -e "SELECT id, employee_id, month_year, basic_salary, gross_salary, net_salary, status FROM payslips ORDER BY id;"

Write-Host "`nStep 2: Showing employee salary data..." -ForegroundColor Green
& $mysqlCommand -u $username -p$password $database -e "SELECT id, CONCAT(first_name, ' ', last_name) as name, basic_salary, da, hra, other_allowance, salary FROM employees ORDER BY id;"

Write-Host "`nStep 3: Deleting GENERATED payslips..." -ForegroundColor Green
$confirm = Read-Host "Are you sure you want to delete all GENERATED payslips? (y/n)"
if ($confirm -eq 'y') {
    & $mysqlCommand -u $username -p$password $database -e "DELETE FROM payslips WHERE status = 'GENERATED';"
    Write-Host "✓ Generated payslips deleted!" -ForegroundColor Green
    
    Write-Host "`nStep 4: Verifying deletion..." -ForegroundColor Green
    & $mysqlCommand -u $username -p$password $database -e "SELECT COUNT(*) as remaining_payslips FROM payslips;"
    
    Write-Host "`n============================================" -ForegroundColor Cyan
    Write-Host "NEXT STEPS:" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "1. Open your browser and go to: http://localhost:8080" -ForegroundColor White
    Write-Host "2. Go to Payslips page" -ForegroundColor White
    Write-Host "3. Click 'Generate Payslip' for March 2026" -ForegroundColor White
    Write-Host "4. The new payslips will show REAL salary data!" -ForegroundColor Green
    Write-Host ""
    Write-Host "OR use the API directly:" -ForegroundColor Yellow
    Write-Host "POST http://localhost:8080/api/payslips/bulk-generate?monthYear=2026-03" -ForegroundColor Cyan
    Write-Host ""
} else {
    Write-Host "Operation cancelled." -ForegroundColor Yellow
}
