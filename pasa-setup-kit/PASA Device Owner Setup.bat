@echo off
chcp 65001 >nul 2>&1
title PASA Sentinel — Device Owner Setup Kit

:: ── Check if PowerShell is available ──────────────────────────────────
where powershell >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] PowerShell is required but not found on this system.
    echo Please install PowerShell from: https://aka.ms/pscore6
    pause
    exit /b 1
)

:: ── Run the PowerShell wizard with required execution policy ───────────
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1" -KitDir "%~dp0"
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Setup wizard exited with an error. Check the output above.
    pause
)
