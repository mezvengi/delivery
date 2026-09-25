@echo off
REM ==============================================================================
REM Database Backup Script for Sour El Ghozlane Delivery (Windows)
REM ==============================================================================

setlocal enabledelayedexpansion

set BACKUP_DIR=.\backups
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value') do set dt=%%I
set TIMESTAMP=%dt:~0,4%%dt:~4,2%%dt:~6,2%_%dt:~8,2%%dt:~10,2%%dt:~12,2%
set BACKUP_FILE=%BACKUP_DIR%\sour_delivery_%TIMESTAMP%.sql

echo [%DATE% %TIME%] Starting backup of sour_delivery database...
docker exec -t sour_postgres pg_dump -U sour_user sour_delivery > "%BACKUP_FILE%"

if %ERRORLEVEL% equ 0 (
    echo [%DATE% %TIME%] Backup created successfully: %BACKUP_FILE%
) else (
    echo [%DATE% %TIME%] Backup failed! Please check docker status.
)

endlocal
