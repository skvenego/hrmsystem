# Auto-Fix User Profile Data Script
# This will add missing columns and update existing users with default data

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  User Profile Auto-Fix Script" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This script will:" -ForegroundColor Yellow
Write-Host "1. Add phone/address columns if missing" -ForegroundColor White
Write-Host "2. Update existing users with default data" -ForegroundColor White
Write-Host "3. Show all users with their profile data" -ForegroundColor White
Write-Host ""

$mysqlUser = Read-Host "MySQL username (default: root)"
if ([string]::IsNullOrWhiteSpace($mysqlUser)) { $mysqlUser = "root" }

$mysqlPassword = Read-Host "MySQL password" -AsSecureString
$plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($mysqlPassword)
)

$database = Read-Host "Database name (default: hrmsystem)"
if ([string]::IsNullOrWhiteSpace($database)) { $database = "hrmsystem" }

Write-Host ""
Write-Host "Running auto-fix on database '$database'..." -ForegroundColor Yellow

$sql = @"
USE $database;

-- Add columns if they don't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(50) AFTER email;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(50) AFTER first_name;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20) AFTER last_name;
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT AFTER phone;
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100) AFTER address;
ALTER TABLE users ADD COLUMN IF NOT EXISTS position VARCHAR(100) AFTER department;

-- Update NULL values
UPDATE users 
SET 
    first_name = COALESCE(first_name, 'User'),
    last_name = COALESCE(last_name, SUBSTRING_INDEX(username, '@', 1)),
    phone = COALESCE(phone, '+0000000000'),
    address = COALESCE(address, 'Not provided'),
    department = COALESCE(department, 'General'),
    position = COALESCE(position, 'Employee')
WHERE first_name IS NULL OR last_name IS NULL OR phone IS NULL OR address IS NULL;

-- Show results
SELECT id, username, email, first_name, last_name, phone, address, department, position, role FROM users ORDER BY id;
"@

try {
    $env:MYSQL_PWD = $plainPassword
    $result = mysql -u $mysqlUser $database -e $sql 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ SUCCESS!" -ForegroundColor Green
        Write-Host "Database updated successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Results:" -ForegroundColor Cyan
        Write-Host $result -ForegroundColor White
        Write-Host ""
        Write-Host "Next Steps:" -ForegroundColor Yellow
        Write-Host "1. Refresh your Profile page - should show data now!" -ForegroundColor White
        Write-Host "2. Go to Settings page - should also show data!" -ForegroundColor White
        Write-Host "3. You can edit and save your real phone/address" -ForegroundColor White
    } else {
        throw "MySQL command failed with exit code $LASTEXITCODE"
    }
}
catch {
    Write-Host ""
    Write-Host "✗ ERROR!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "1. Make sure MySQL is running" -ForegroundColor White
    Write-Host "2. Check database name is correct" -ForegroundColor White
    Write-Host "3. Verify MySQL credentials" -ForegroundColor White
    Write-Host "4. Try manual SQL: source auto_fix_user_profile.sql" -ForegroundColor White
}
finally {
    $env:MYSQL_PWD = $null
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
