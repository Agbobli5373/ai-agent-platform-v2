@echo off
REM Load environment variables from .env file and set them for the current session

echo Loading environment variables from .env file...

for /f "usebackq tokens=1,2 delims==" %%a in (".env") do (
    set "%%a=%%b"
    echo Set %%a=%%b
)

echo.
echo Environment variables loaded successfully!
echo.
echo You can now run: mvnw.cmd quarkus:dev
echo.
echo Or run this script with your command:
echo   set-env.bat mvnw.cmd quarkus:dev
echo.

if not "%~1"=="" (
    echo Running command: %*
    %*
)
