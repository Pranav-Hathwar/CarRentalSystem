# CarRental System — Presentation Guide

This file contains the exact commands and quick demo steps to run and present the CarRental web app locally on Windows (PowerShell).

## Prerequisites
- Java 17+ installed and on PATH
- Apache Tomcat 11 installed (recommended default: `C:\Program Files\Apache Software Foundation\Tomcat 11.0`)
- The project is located at:

```
C:\Users\Pranav\OneDrive\Desktop\java_el_car_rental_system
```

## Quick Start (build, deploy & run)
Open PowerShell (recommended: Run as Administrator), then:

```powershell
cd 'C:\Users\Pranav\OneDrive\Desktop\java_el_car_rental_system'
.\BUILD_AND_RUN.ps1
```

What the script does:
- Stops Tomcat (if running)
- Compiles Java sources into `WEB-INF/classes`
- Copies required libraries (H2) into `WEB-INF/lib`
- Deploys the webapp to Tomcat `webapps/ROOT`
- Starts Tomcat

After the script finishes, open the app at:

- http://localhost:8080
- Cars page: http://localhost:8080/cars.html
- Admin dashboard: http://localhost:8080/admin.html
- Login page: http://localhost:8080/login.html

## Demo flow to show (recommended)
1. Open browser to `http://localhost:8080`.
2. Click **Login**, sign in with admin credentials:
   - Email: `admin@example.com`
   - Password: `admin`
3. Navigate to `cars.html`. As an admin you should see a green `+ Add Car` button.
4. Click `+ Add Car`, fill the form and submit. The new car appears immediately in the listing.

This demonstrates role-based UI and the admin-only endpoint `/api/admin/cars`.

## Useful API test commands (PowerShell)
Use these when you need to demo backend calls or if the UI is not accessible.

- Login (keep cookies in `$session`):
```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" `
  -Method POST `
  -Headers @{ 'Content-Type' = 'application/json' } `
  -Body '{"email":"admin@example.com","password":"admin"}' `
  -WebSession $session | ConvertFrom-Json
```

- Add a car as admin:
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/admin/cars" `
  -Method POST `
  -Headers @{ 'Content-Type' = 'application/json' } `
  -Body '{"name":"Demo Car","price":99,"image":"https://example.com/demo.jpg","features":"Demo, Auto"}' `
  -WebSession $session | ConvertFrom-Json
```

- List cars (JSON):
```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/cars" -Headers @{ 'Content-Type' = 'application/json' } |
  Select-Object -ExpandProperty Content
```

## Stop / Start Tomcat manually
If you need to stop or start Tomcat yourself:

```powershell
& 'C:\Program Files\Apache Software Foundation\Tomcat 11.0\bin\shutdown.bat'
& 'C:\Program Files\Apache Software Foundation\Tomcat 11.0\bin\startup.bat'
```

If `CATALINA_HOME` is not set, set it for this session:

```powershell
$env:CATALINA_HOME = 'C:\Program Files\Apache Software Foundation\Tomcat 11.0'
```

To persist the variable (optional):
```powershell
setx CATALINA_HOME "C:\Program Files\Apache Software Foundation\Tomcat 11.0"
```

## Troubleshooting (common)
- Port 8080 already in use: find and kill process that holds 8080
  ```powershell
  netstat -ano | Select-String ':8080'
  Stop-Process -Id <PID> -Force
  ```
- H2 DB locked when trying to inspect the DB: stop Tomcat before opening DB file.
- Add Car button not visible: ensure you are logged in as admin and `localStorage.currentUser.role === 'ADMIN'` in browser DevTools.
- Build error referencing servlet classes: ensure Tomcat 11 (Jakarta namespace) and `CATALINA_HOME` are set.

## Contact / Notes
- Admin credentials for demo: `admin@example.com` / `admin`
- The project uses an H2 file-based DB (placed in the user's home). When Tomcat runs, the DB file is locked.

Good luck with your presentation — if you'd like, I can also open a screenshot of the running app or generate a short PDF export of this README.
