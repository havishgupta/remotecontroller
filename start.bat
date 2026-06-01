@echo off
title Presentation Remote Server
cd server
if not exist "node_modules\" (
    echo Installing dependencies for the first time...
    call npm install
)
node server.js
pause
