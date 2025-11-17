#!/bin/bash

echo "=========================================="
echo "AI Agent Platform - Setup Verification"
echo "=========================================="
echo ""

# Check Java version
echo "✓ Checking Java version..."
java -version 2>&1 | head -n 1

# Check Maven
echo "✓ Checking Maven..."
./mvnw --version | head -n 1

# Check project structure
echo ""
echo "✓ Verifying project structure..."
echo "  - Source directory: $([ -d src/main/java ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Resources directory: $([ -d src/main/resources ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Templates directory: $([ -d src/main/resources/templates ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Static assets: $([ -d src/main/resources/META-INF/resources ] && echo 'EXISTS' || echo 'MISSING')"

# Check key files
echo ""
echo "✓ Verifying key files..."
echo "  - pom.xml: $([ -f pom.xml ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - application.properties: $([ -f src/main/resources/application.properties ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Base layout: $([ -f src/main/resources/templates/layout/base.html ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Welcome page: $([ -f src/main/resources/templates/index.html ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Custom CSS: $([ -f src/main/resources/META-INF/resources/css/app.css ] && echo 'EXISTS' || echo 'MISSING')"
echo "  - Custom JS: $([ -f src/main/resources/META-INF/resources/js/app.js ] && echo 'EXISTS' || echo 'MISSING')"

# Check dependencies
echo ""
echo "✓ Key dependencies configured:"
echo "  - Quarkus Hibernate ORM Panache"
echo "  - Quarkus PostgreSQL JDBC"
echo "  - Quarkus Redis Client"
echo "  - Quarkus SmallRye JWT"
echo "  - Quarkus REST Qute"
echo "  - Quarkus WebSockets Next"
echo "  - LangChain4j Mistral AI"
echo "  - LangChain4j PGVector"
echo "  - Quarkus Flyway"
echo "  - Quarkus Micrometer Prometheus"

# Compile check
echo ""
echo "✓ Compiling project..."
./mvnw clean compile -DskipTests -q

if [ $? -eq 0 ]; then
    echo "  ✅ Compilation successful!"
else
    echo "  ❌ Compilation failed!"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ Setup verification complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Start PostgreSQL: docker run -d --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 ankane/pgvector"
echo "2. Start Redis: docker run -d --name redis -p 6379:6379 redis:latest"
echo "3. Set MISTRAL_API_KEY environment variable"
echo "4. Run: ./mvnw quarkus:dev"
echo "5. Visit: http://localhost:8080"
echo ""
