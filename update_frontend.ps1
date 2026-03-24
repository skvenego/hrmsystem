# Manual Frontend Update Script
# This script copies the updated Attendance.jsx to show changes immediately

Write-Host "=== Updating Frontend Files ===" -ForegroundColor Green

# Stop the running Spring Boot app (if any)
Write-Host "Stopping Spring Boot application..." -ForegroundColor Yellow
Stop-Process -Name "java" -ErrorAction SilentlyContinue

# Copy updated source files
$sourceFile = "c:\Users\GCV\Projects\hrmsystem\src\main\resources\hrmsystem-frontend\src\pages\Attendance.jsx"
$targetDir = "c:\Users\GCV\Projects\hrmsystem\target\classes\hrmsystem-frontend\src\pages\"

if (Test-Path $sourceFile) {
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    }
    
    Copy-Item -Path $sourceFile -Destination $targetDir -Force
    Write-Host "✓ Attendance.jsx updated successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Source file not found!" -ForegroundColor Red
}

Write-Host "`n=== Starting Application ===" -ForegroundColor Green
Write-Host "Please restart the Spring Boot application manually" -ForegroundColor Yellow
Write-Host "Command: .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
