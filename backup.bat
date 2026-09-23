@echo off
setlocal
set TIMESTAMP=%date:~-4%%date:~3,2%%date:~0,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_DIR=%USERPROFILE%\SOUR_BACKUPS

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

echo [BACKUP] جارٍ إنشاء نسخة احتياطية لقاعدة البيانات...
docker exec -i sour_postgres pg_dump -U sour_admin sour_delivery > "%BACKUP_DIR%\sour_backup_%TIMESTAMP%.sql"

echo [SUCCESS] تم حفظ النسخة الاحتياطية بنجاح في:
echo %BACKUP_DIR%\sour_backup_%TIMESTAMP%.sql
pause
