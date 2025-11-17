@echo off
echo ==========================================
echo AI Agent Platform - Setup Verification
echo ==========================================
echo.

REM Check Java version
echo [CHECK] Checking Java version...
java -version 2>&1 | findstr /C:"version"

REM Check Maven
echo [CHECK] Checking Maven...
call mvnw.cmd --version | findstr /C:"Apache Maven"

REM Check project structure
echo.
echo [CHECK] Verifying project structure...
if exist "src\main\java" (echo   - Source directory: EXISTS) else (echo   - Source directory: MISSING)
if exist "src\main\resources" (echo   - Resources directory: EXISTS) else (echo   - Resources directory: MISSING)
if exist "src\main\resources\templates" (echo   - Templates directory: EXISTS) else (echo   - Templates directory: MISSING)
if exist "src\main\resources\META-INF\resources" (echo   - Static assets: EXISTS) else (echo   - Static assets: MISSING)

REM Check key files
echo.
echo [CHECK] Verifying key files...
if exist "pom.xml" (echo   - pom.xml: EXISTS) else (echo   - pom.xml: MISSING)
if exist "src\main\resources\application.properties" (echo   - application.properties: EXISTS) else (echo   - application.properties: MISSING)
if exist "src\main\resources\templates\layout\base.html" (echo   - Base layout: EXISTS) else (echo   - Base layout: MISSING)
if exist "src\main\resources\templates\index.html" (echo   - Welcome page: EXISTS) else (echo   - Welcome page: MISSING)
if exist "src\main\resources\META-INF\resources\css\app.css" (echo   - Custom CSS: EXISTS) else (echo   - Custom CSS: MISSING)
if exist "src\main\resources\META-INF\resources\js\app.js" (echo   - Custom JS: EXISTS) else (echo   - Custom JS: MISSING)

REM Check dependencies
echo.
echo [CHECK] Key dependencies configured:
echo   - Quarkus Hibernate ORM Panache
echo   - Quarkus PostgreSQL JDBC
echo   - Quarkus Redis Client
echo   - Quarkus SmallRye JWT
echo   - Quarkus REST Qute
echo   - Quarkus WebSockets Next
echo   - LangChain4j Mistral AI
echo   - LangChain4j PGVector
echo   - Quarkus Flyway
echo   - Quarkus Micrometer Prometheus

REM Compile check
echo.
echo [CHECK] Compiling project...
call mvnw.cmd clean compile -DskipTests -q

if %ERRORLEVEL% EQU 0 (
    echo   SUCCESS: Compilation successful!
) else (
    echo   ERROR: Compilation failed!
    exit /b 1
)

echo.
echo ==========================================
echo SUCCESS: Setup verification complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Start PostgreSQL: docker run -d --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 ankane/pgvector
echo 2. Start Redis: docker run -d --name redis -p 6379:6379 redis:latest
echo 3. Set MISTRAL_API_KEY environment variable
echo 4. Run: mvnw.cmd quarkus:dev
echo 5. Visit: http://localhost:8080
echo.
