@echo off
title Presentation Remote Server
echo ==========================================
echo    Wireless Presentation Remote Server
echo ==========================================
echo.
echo Your Local IP Addresses (Enter one of these in the app):
ipconfig | findstr /i "ipv4"
echo.
cd server
if not exist "node_modules\" (
    echo Installing dependencies for the first time...
    call npm install
)
echo.
echo Starting server...
node server.js
pause
