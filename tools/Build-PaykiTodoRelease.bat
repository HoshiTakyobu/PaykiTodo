@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Build-PaykiTodoRelease.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo Release 构建失败，退出码 %EXIT_CODE%。
) else (
  echo.
  echo Release 构建完成。
)
pause
exit /b %EXIT_CODE%
