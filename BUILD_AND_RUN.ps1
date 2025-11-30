$ErrorActionPreference = "Stop"

# Configuration
$tomcatHome = "C:\Program Files\Apache Software Foundation\Tomcat 11.0"
$projectRoot = Get-Location
$webContent = "$projectRoot\WebContent"
$classesDir = "$webContent\WEB-INF\classes"
$libDir = "$webContent\WEB-INF\lib"
$tomcatLib = "$tomcatHome\lib"
$servletApi = "$tomcatLib\servlet-api.jar"
if (-not $env:CATALINA_HOME) {
    $env:CATALINA_HOME = $tomcatHome
    Write-Host "Setting CATALINA_HOME to $env:CATALINA_HOME"
} else {
    Write-Host "CATALINA_HOME already set to $env:CATALINA_HOME"
}

# Try to locate a servlet API jar inside Tomcat lib if the default name isn't present
try {
    $servletCandidate = Get-ChildItem -Path $tomcatLib -Filter "*servlet*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($servletCandidate) { $servletApi = $servletCandidate.FullName }
} catch {}
$h2Jar = "$projectRoot\h2-2.2.224.jar"

# Check Prerequisites
if (-not (Test-Path $tomcatHome)) {
    Write-Error "Tomcat not found at $tomcatHome"
}
if (-not (Test-Path $servletApi)) {
    Write-Error "servlet-api.jar not found at $servletApi"
}

# 1. Stop Tomcat
Write-Host "Stopping Tomcat..." -ForegroundColor Cyan
& "$tomcatHome\bin\shutdown.bat"
Write-Host "Waiting for Tomcat to stop (10 seconds)..."
Start-Sleep -Seconds 10

# 2. Clean
Write-Host "Cleaning..." -ForegroundColor Cyan
try {
    if (Test-Path $classesDir) { Remove-Item -Recurse -Force $classesDir -ErrorAction Stop }
    if (Test-Path $libDir) { Remove-Item -Recurse -Force $libDir -ErrorAction Stop }
}
catch {
    Write-Warning "Could not remove some files. Tomcat might still be locking them. Attempting to continue..."
}

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $libDir | Out-Null

# 3. Prepare Libraries
Write-Host "Preparing Libraries..." -ForegroundColor Cyan
if (-not (Test-Path $h2Jar)) {
    $h2Jar = Get-ChildItem -Path $projectRoot -Filter "h2*.jar" -Recurse | Select-Object -First 1 -ExpandProperty FullName
}
if ($h2Jar) {
    Copy-Item $h2Jar "$libDir\"
    Write-Host "Copied H2 Jar: $h2Jar"
}
else {
    Write-Error "H2 Database JAR not found!"
}

# 4. Compile
Write-Host "Compiling..." -ForegroundColor Cyan
$sources = Get-ChildItem -Path "$projectRoot\src" -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$classpath = "$servletApi;$libDir\*"
javac -cp $classpath -d $classesDir $sources
if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation Failed!"
}
Write-Host "Compilation Successful." -ForegroundColor Green

# 5. Deploy
Write-Host "Deploying..." -ForegroundColor Cyan
$deployPath = "$tomcatHome\webapps\ROOT"
try {
    if (Test-Path $deployPath) { Remove-Item -Recurse -Force $deployPath -ErrorAction Stop }
}
catch {
    Write-Warning "Could not remove existing deployment. Tomcat might still be locking it. Please manually stop Tomcat if this fails."
    # Try to continue anyway, maybe we can overwrite
}
Copy-Item -Recurse -Force $webContent $deployPath
Write-Host "Deployed to $deployPath" -ForegroundColor Green

# 6. Start Tomcat
Write-Host "Starting Tomcat..." -ForegroundColor Cyan
& "$tomcatHome\bin\startup.bat"

Write-Host "Done! Access at http://localhost:8080" -ForegroundColor Green
