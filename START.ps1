# CarRental System - One Command Launcher
# Run: .\START.ps1

Write-Host "================================" -ForegroundColor Cyan
Write-Host "  CarRental System - One Click" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build and Deploy
Write-Host "[1/3] Building and deploying application..." -ForegroundColor Yellow
& .\BUILD_AND_RUN.ps1

# Step 2: Wait for Tomcat to start
Write-Host "[2/3] Waiting for server to fully start (10 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Step 3: Open in browser
Write-Host "[3/3] Opening application in browser..." -ForegroundColor Yellow
Start-Process "http://localhost:8080/cars.html"

Write-Host ""
Write-Host "================================" -ForegroundColor Green
Write-Host "  Server is running!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""
Write-Host "Admin Login:" -ForegroundColor Cyan
Write-Host "  Email: admin@example.com" -ForegroundColor White
Write-Host "  Password: admin" -ForegroundColor White
Write-Host ""
Write-Host "App URL: http://localhost:8080" -ForegroundColor Cyan
Write-Host "To stop Tomcat, run: shutdown" -ForegroundColor Gray
Write-Host ""
