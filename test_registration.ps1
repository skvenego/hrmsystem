# Test Registration API with Phone and Address
Write-Host "Testing Registration with Phone and Address..." -ForegroundColor Cyan

# Create test user data
$testUser = @{
    username = "testuser$(Get-Random -Maximum 9999)"
    email = "test$(Get-Random -Maximum 9999)@example.com"
    password = "TestPass123!"
    role = "EMPLOYEE"
    phone = "+1-555-123-4567"
    address = "123 Main Street, New York, NY 10001"
} | ConvertTo-Json

Write-Host "`nSending registration request..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $testUser -ContentType "application/json"
    
    Write-Host "`nSUCCESS: Registration Successful!" -ForegroundColor Green
    Write-Host "Username: $($response.username)" -ForegroundColor White
    Write-Host "Email: $($response.email)" -ForegroundColor White
    Write-Host "User ID: $($response.id)" -ForegroundColor White
    
    # Now fetch the user to verify phone and address are stored
    Write-Host "`nFetching user details to verify storage..." -ForegroundColor Yellow
    $headers = @{
        "Authorization" = "Bearer $($response.token)"
        "Content-Type" = "application/json"
    }
    
    $userData = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/users/$($response.id)" -Method Get -Headers $headers
    
    Write-Host "`nUser Data from Database:" -ForegroundColor Cyan
    Write-Host "Phone: $($userData.phone)" -ForegroundColor $(if ($userData.phone) { "Green" } else { "Red" })
    Write-Host "Address: $($userData.address)" -ForegroundColor $(if ($userData.address) { "Green" } else { "Red" })
    Write-Host "Department: $($userData.department)" -ForegroundColor White
    Write-Host "Position: $($userData.position)" -ForegroundColor White
    
    if ($userData.phone -eq $testUser.phone -and $userData.address -eq $testUser.address) {
        Write-Host "`nSUCCESS: Phone and Address are correctly stored in MySQL!" -ForegroundColor Green
    } else {
        Write-Host "`nWARNING: Data mismatch detected!" -ForegroundColor Red
    }
} catch {
    Write-Host "`nError: $($_.Exception.Message)" -ForegroundColor Red
}
