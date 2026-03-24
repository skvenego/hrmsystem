# Script to insert sample data into HRM database
$mysqlPath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

# Check if MySQL is available in PATH
if (Get-Command mysql -ErrorAction SilentlyContinue) {
    $mysqlCommand = "mysql"
} elseif (Test-Path $mysqlPath) {
    $mysqlCommand = $mysqlPath
} else {
    Write-Host "MySQL not found. Please add MySQL to PATH or install MySQL." -ForegroundColor Red
    Write-Host "Alternatively, run this SQL manually:" -ForegroundColor Yellow
    Write-Host "mysql -u root -p hrm_db < insert_sample_data.sql" -ForegroundColor Cyan
    exit 1
}

Write-Host "Inserting sample data into hrm_db..." -ForegroundColor Green

# Execute the SQL file
& $mysqlCommand -u root -pSachin@123 hrm_db < insert_sample_data.sql

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Sample data inserted successfully!" -ForegroundColor Green
    Write-Host "`nYou can now login with these credentials:" -ForegroundColor Cyan
    Write-Host "Note: You'll need to create user accounts separately via the registration page" -ForegroundColor Yellow
} else {
    Write-Host "✗ Error inserting data. Check MySQL connection." -ForegroundColor Red
}
