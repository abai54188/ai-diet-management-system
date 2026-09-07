@echo off
title AI Diet Management System

setlocal

rem Use the directory of this script as working directory
rem (backend SQLite relative path "data/diet.db" depends on it)
cd /d "%~dp0"

rem Ensure the SQLite data directory exists
if not exist "data" mkdir "data"

rem Locate the jar: prefer same directory, then backend/target
set "JAR=%~dp0diet-backend-1.0.0.jar"
if not exist "%JAR%" set "JAR=%~dp0backend\target\diet-backend-1.0.0.jar"

if not exist "%JAR%" (
    echo [ERROR] Cannot find diet-backend-1.0.0.jar.
    echo Build it first: npm run build + mvn package,
    echo or place the jar next to this script and retry.
    pause
    exit /b 1
)

echo Checking Java environment...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17+ from:
    echo         https://adoptium.net/
    pause
    exit /b 1
)

if "%AI_API_KEY%"=="" (
    echo [INFO] AI_API_KEY is not set. AI features will be disabled.
    echo        Other features are unaffected.
)

echo.
echo Starting AI Diet Management System...
echo Browser will open http://localhost:8080 once ready.
echo Close this window or press Ctrl+C to stop.
echo.

rem Open browser after 8 seconds (allow backend + DB init time)
start "" cmd /c "timeout /t 8 /nobreak >nul & start http://localhost:8080"

rem Run jar in foreground; Ctrl+C or closing the window stops it
java -jar "%JAR%"

pause