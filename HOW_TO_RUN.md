# How to Run the Car Rental System

## Quick Start

1. **Double-click `RUN_APPLICATION.bat`** in your project folder
2. Wait for the script to compile, deploy, and start Tomcat (about 30 seconds)
3. Open your browser and go to: **http://localhost:8080**

## Manual Steps (if the script doesn't work)

### Step 1: Compile the Application
Open PowerShell in your project folder and run:
```powershell
cd "C:\Users\Pranav\OneDrive\Desktop\java_el_car_rental_system"
mkdir WebContent\WEB-INF\classes -Force
Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Out-File -Encoding ASCII sources.txt
javac -cp "WebContent\WEB-INF\lib\servlet-api.jar;WebContent\WEB-INF\lib\h2-2.2.224.jar" -d WebContent\WEB-INF\classes "@sources.txt"
Remove-Item sources.txt
```

### Step 2: Deploy to Tomcat
```powershell
$env:CATALINA_HOME = "C:\Program Files\Apache Software Foundation\Tomcat 11.0"
& "$env:CATALINA_HOME\bin\shutdown.bat"
Remove-Item -Recurse -Force "$env:CATALINA_HOME\webapps\ROOT" -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force "WebContent" "$env:CATALINA_HOME\webapps\ROOT"
```

### Step 3: Start Tomcat
```powershell
& "$env:CATALINA_HOME\bin\startup.bat"
```

Wait 10-15 seconds, then open: **http://localhost:8080**

## Troubleshooting

### Problem: "Connection Refused" or "Cannot connect"
- **Solution**: Wait 15-20 seconds after starting Tomcat. Check if Tomcat console window shows any errors.

### Problem: 404 Error
- **Solution**: Make sure `index.html` exists in `WebContent` folder. The deployment might have failed.

### Problem: Compilation Errors
- **Solution**: Make sure `servlet-api.jar` is in `WebContent/WEB-INF/lib/` folder.

### Problem: Database Connection Error
- **Solution**: The H2 database will be created automatically. Check that you have write permissions in your user directory.

### Problem: Tomcat Won't Start
- **Solution**: Check if port 8080 is already in use:
  ```powershell
  netstat -ano | findstr :8080
  ```
  If something is using port 8080, stop it or change Tomcat's port in `server.xml`.

## Default Login Credentials

- **Admin**: admin@example.com / admin
- **User**: user@example.com / user

## Access Points

- **Homepage**: http://localhost:8080
- **Cars Page**: http://localhost:8080/cars.html
- **Login**: http://localhost:8080/login.html
- **Signup**: http://localhost:8080/signup.html

## Features

✅ 12 predefined cars  
✅ Search and filter functionality  
✅ User authentication  
✅ Booking system  
✅ Beautiful modern UI  
✅ Responsive design

## Need Help?

If you're still having issues, please check:
1. Java 17+ is installed (`java -version`)
2. Tomcat 11.0 is installed at the expected path
3. All JAR files are in `WebContent/WEB-INF/lib/`
4. No firewall blocking port 8080
