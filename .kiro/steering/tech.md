# Technology Stack

## Backend

- **Framework**: Quarkus 3.x (Java)
- **ORM**: Hibernate Panache for simplified data access
- **AI Framework**: LangChain4j with Quarkus extensions
- **LLM Provider**: Mistral AI (primary), extensible to other providers
- **Database**: PostgreSQL 15+ with pgvector extension for vector embeddings
- **Caching**: Redis for sessions and chat memory
- **Security**: Quarkus Security with JWT/OAuth2

## Frontend

- **Template Engine**: Qute (server-side rendering)
- **CSS Framework**: Tailwind CSS for modern, responsive UI
- **JavaScript**: Alpine.js for interactive components
- **Icons**: Heroicons or Lucide icons
- **Real-Time**: WebSocket for streaming agent responses

## Infrastructure

- **Containerization**: Docker
- **Orchestration**: Kubernetes-ready with HPA support
- **Monitoring**: Micrometer with Prometheus metrics
- **Build Tool**: Maven or Gradle

## Common Commands

### Development
```bash
# Run in dev mode with live reload
./mvnw quarkus:dev

# Run tests
./mvnw test

# Build application
./mvnw clean package
```

### Database
```bash
# Start PostgreSQL with pgvector (via Docker)
docker run -d --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 ankane/pgvector

# Start Redis
docker run -d --name redis -p 6379:6379 redis:latest
```

### Docker
```bash
# Build Docker image
docker build -t ai-agent-platform .

# Run with docker-compose
docker-compose up -d
```

### Production Build
```bash
# Build native executable (optional)
./mvnw package -Pnative

# Build JVM package
./mvnw clean package -DskipTests
```

## Key Dependencies

### Core Quarkus Extensions
- `io.quarkus:quarkus-hibernate-orm-panache` - Database ORM with Panache
- `io.quarkus:quarkus-jdbc-postgresql` - PostgreSQL JDBC driver
- `io.quarkus:quarkus-redis-client` - Redis client for caching
- `io.quarkus:quarkus-smallrye-jwt` - JWT authentication (SmallRye JWT)
- `io.quarkus:quarkus-oidc` - OAuth2/OIDC support (alternative to JWT)
- `io.quarkus:quarkus-rest-qute` - Qute template engine for REST
- `io.quarkiverse.qute.web:quarkus-qute-web` - Qute web extension for direct template serving
- `io.quarkus:quarkus-websockets-next` - WebSocket support (next-gen)
- `io.quarkus:quarkus-micrometer-registry-prometheus` - Prometheus metrics
- `io.quarkus:quarkus-rest-jackson` - REST with Jackson JSON

### LangChain4j Extensions
- `io.quarkiverse.langchain4j:quarkus-langchain4j-core` - Core LangChain4j integration
- `io.quarkiverse.langchain4j:quarkus-langchain4j-mistral-ai` - Mistral AI provider
- `io.quarkiverse.langchain4j:quarkus-langchain4j-pgvector` - PostgreSQL vector store

### Database Migration
- `io.quarkus:quarkus-flyway` or `io.quarkus:quarkus-liquibase` - Database migrations

## Example Maven Dependencies

```xml
<dependencies>
    <!-- Core Quarkus -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-hibernate-orm-panache</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-jdbc-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-redis-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-jwt</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-qute</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-websockets-next</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- LangChain4j -->
    <dependency>
        <groupId>io.quarkiverse.langchain4j</groupId>
        <artifactId>quarkus-langchain4j-mistral-ai</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkiverse.langchain4j</groupId>
        <artifactId>quarkus-langchain4j-pgvector</artifactId>
    </dependency>
</dependencies>
```
