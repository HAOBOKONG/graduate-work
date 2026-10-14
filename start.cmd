@echo off
setlocal
title Secondhand Market

if not exist "%~dp0scripts\start-backend.ps1" (
    echo Backend startup script is missing.
    pause
    exit /b 1
)
if not exist "%~dp0scripts\start-frontend.ps1" (
    echo Frontend startup script is missing.
    pause
    exit /b 1
)

call :port 5432
if errorlevel 1 (
    echo PostgreSQL is not running on port 5432.
    echo Start Docker and the post-demo container, then run this script again.
    pause
    exit /b 1
)

set "SERVER_PORT=18080"
call :port 18080
if errorlevel 1 (
    echo Starting backend. Enter the database password in the backend window.
    start "Market Backend - 18080" powershell.exe -NoProfile -NoExit -ExecutionPolicy Bypass -File "%~dp0scripts\start-backend.ps1"
) else (
    echo Port 18080 is already in use. Skipping backend startup.
)

call :port 5173
if errorlevel 1 (
    echo Starting frontend.
    start "Market Frontend - 5173" powershell.exe -NoProfile -NoExit -ExecutionPolicy Bypass -File "%~dp0scripts\start-frontend.ps1"
) else (
    echo Port 5173 is already in use. Skipping frontend startup.
)

echo.
echo Website: http://localhost:5173/
echo Admin:   http://localhost:5173/admin
echo Keep the backend and frontend windows open while using the website.
exit /b 0

:port
powershell.exe -NoProfile -Command "$client = New-Object System.Net.Sockets.TcpClient; try { $client.Connect('127.0.0.1', %1); exit 0 } catch { exit 1 } finally { $client.Dispose() }"
exit /b %errorlevel%
