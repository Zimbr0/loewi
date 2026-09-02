@echo off
cd /d "%~dp0"
echo === Lowie App Upload ===
echo.

set /p MSG="Kurze Beschreibung der Aenderung (Enter fuer Standardtext): "
if "%MSG%"=="" set MSG=Update

git add -A
git commit -m "%MSG%"
if errorlevel 1 (
    echo.
    echo Keine Aenderungen zum Hochladen gefunden.
    pause
    exit /b
)

git push origin main

echo.
echo === Fertig! Aenderungen sind auf GitHub. ===
pause
