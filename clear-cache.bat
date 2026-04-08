@echo off
echo Clearing browser cache and restarting HRM System...
echo.

echo 1. Killing Chrome processes...
taskkill /F /IM chrome.exe >nul 2>&1

echo 2. Clearing DNS cache...
ipconfig /flushdns >nul 2>&1

echo 3. Stopping Spring Boot...
taskkill /F /IM java.exe >nul 2>&1

echo 4. Waiting 3 seconds...
timeout /t 3 >nul

echo 5. Starting HRM System...
cd /d "C:\Users\GCV\Projects\hrmsystem"
.\mvnw.cmd spring-boot:run

pause
