# Quick Database Cleanup - No Password Prompt
# Assumes you're using default MySQL installation

$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$sqlFile = "$(Get-Location)\cleanup_database.sql"

Write-Host "Running database cleanup..." -ForegroundColor Cyan

# Try with empty password first, then prompt if fails
try {
    $output = & $mysqlPath -u root hrmsystem < $sqlFile 2>&1
    Write-Host "Cleanup completed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Output:" -ForegroundColor Yellow
    Write-Host $output
} catch {
    Write-Host "Error occurred: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run manually in MySQL Workbench:" -ForegroundColor Yellow
    Write-Host "File: $sqlFile"
}
