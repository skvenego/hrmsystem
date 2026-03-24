# PowerShell script to cleanup payslips
$mysqlUser = "root"
$mysqlPassword = "Sachin@123"
$mysqlDatabase = "hrmsystem"
$sqlFile = "c:\Users\GCV\Projects\hrmsystem\cleanup_payslips.sql"

Write-Host "=== Cleaning up Payslips ===" -ForegroundColor Cyan

# Try to find mysql.exe (common installation paths)
$mysqlPaths = @(
    "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe",
    "C:\xampp\mysql\bin\mysql.exe",
    "C:\wamp\bin\mysql\mysql8.0.27\bin\mysql.exe"
)

$mysqlExe = $null
foreach ($path in $mysqlPaths) {
    if (Test-Path $path) {
        $mysqlExe = $path
        break
    }
}

if ($mysqlExe) {
    Write-Host "Found MySQL at: $mysqlExe" -ForegroundColor Green
    Get-Content $sqlFile | & $mysqlExe -u $mysqlUser -p$mysqlPassword $mysqlDatabase
    Write-Host "Payslips cleaned successfully!" -ForegroundColor Green
} else {
    Write-Host "MySQL not found. Run SQL manually:" -ForegroundColor Red
    Write-Host ""
    Write-Host "USE hrmsystem;" -ForegroundColor Yellow
    Write-Host "DELETE FROM payslip;" -ForegroundColor Yellow
    Write-Host "COMMIT;" -ForegroundColor Yellow
}
