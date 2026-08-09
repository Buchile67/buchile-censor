@echo off
setlocal
cd /d "%~dp0"
title Buchile Censor

if exist ".venv\Scripts\python.exe" goto run

echo [Buchile] First launch: preparing a private runtime...
where py >nul 2>nul
if not errorlevel 1 (
  py -3 -m venv .venv
) else (
  python -m venv .venv
)

if not exist ".venv\Scripts\python.exe" goto failed
".venv\Scripts\python.exe" -m pip install --upgrade pip
".venv\Scripts\python.exe" -m pip install -r requirements.txt
if errorlevel 1 goto failed

:run
echo [Buchile] Starting... Keep this window open while using the app.
".venv\Scripts\python.exe" -m streamlit run app.py
goto end

:failed
echo.
echo Setup failed. Please install Python 3.10 or newer, then try again.
pause

:end
endlocal
