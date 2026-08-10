@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title Buchile Censor

set "BUCHILE_PYTHON="
if exist ".venv\Scripts\python.exe" set "BUCHILE_PYTHON=%~dp0.venv\Scripts\python.exe"
if defined BUCHILE_PYTHON goto run

if defined LOCALAPPDATA set "BUCHILE_PYTHON=%LOCALAPPDATA%\BuchileRuntime\envs\buchile-censor\python.exe"
if not defined LOCALAPPDATA set "BUCHILE_PYTHON=%~dp0.runtime\envs\buchile-censor\python.exe"
set "BUCHILE_READY=%BUCHILE_PYTHON:\python.exe=\.buchile-ready%"

if not exist "%BUCHILE_PYTHON%" goto install
if not exist "%BUCHILE_READY%" goto install
goto run

:install
(
  call "%~dp0install_autoex.bat"
  if errorlevel 1 goto failed
)
if not exist "%BUCHILE_PYTHON%" goto failed
if not exist "%BUCHILE_READY%" goto failed

:run
echo [Buchile] Starting... Keep this window open while using the app.
"%BUCHILE_PYTHON%" -m streamlit run app.py --browser.gatherUsageStats false
goto end

:failed
echo.
echo Buchile Censor could not start.
echo Copy this entire window when requesting help.
pause

:end
endlocal
