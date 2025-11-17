# AI Agent Platform

A web-based platform that democratizes AI agent creation for non-technical users. Enables small businesses, individuals, and organizations to create, deploy, and manage custom AI agents without coding expertise.

## Quick Start

### 1. Start Docker Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL with pgvector on port **5433**
- Redis on port **6380**

### 2. Set Environment Variables

```bash
# Windows (PowerShell)
$env:MISTRAL_API_KEY="your-api-key-here"

# Windows (CMD)
set MISTRAL_API_KEY=your-api-key-here

# Linux/Mac
export MISTRAL_API_KEY=your-api-key-here
```

### 3. Run the Application

```bash
./mvnw quarkus:dev
```

### 4. Access the Application

- **Web UI**: http://localhost:8080
- **Dev UI**: http://localhost:8080/q/dev

## Technology Stack

### Backend
- **Framework**: Quarkus 3.17.7 (Java 17)
- **ORM**: Hibernate Panache
- **AI Framework**: LangChain4j 0.21.0
- **LLM Provider**: Mistral AI
- **Database**: PostgreSQL 15+ with pgvector extension
- **Caching**: Redis 7
- **Security**: SmallRye JWT
- **Database Migration**: Flyway
- **WebSocket**: Quarkus WebSockets Next
- **Monitoring**: Micrometer with Prometheus

### Frontend
- **Template Engine**: Qute (server-side rendering)
- **CSS Framework**: Tailwind CSS (via CDN)
- **JavaScript**: Alpine.js (via CDN)
- **Icons**: Heroicons

## Project Structure

```
ai-agent-platform/
├── src/main/
│   ├── java/com/platform/
│   │   ├── domain/          # Entity models
│   │   ├── service/         # Business logic
│   │   ├── repository/      # Data access (Panache)
│   │   ├── rest/            # REST API endpoints
│   │   ├── websocket/       # WebSocket endpoints
│   │   ├── ai/              # AI service interfaces
│   │   ├── security/        # Security and auth
│   │   └── exception/       # Exception handlers
│   └── resources/
│       ├── templates/       # Qute templates
│       ├── META-INF/resources/  # Static assets (css, js, images)
│       ├── db/migration/    # Flyway migrations
│       └── application.properties
├── docker-compose.yml       # Docker services configuration
├── DOCKER-SETUP.md         # Detailed Docker guide
└── SETUP.md                # Setup completion guide
```

## Development Commands

```bash
# Run in dev mode with live reload
./mvnw quarkus:dev

# Run tests
./mvnw test

# Build application
./mvnw clean package

# Build native executable (optional)
./mvnw package -Pnative
```

## Docker Management

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose stop

# View logs
docker-compose logs -f

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (⚠️ deletes data)
docker-compose down -v
```

## Database Access

```bash
# Connect to PostgreSQL
docker exec -it ai-agent-postgres psql -U postgres -d ai_agent_platform

# Or from host (if psql installed)
psql -h localhost -p 5433 -U postgres -d ai_agent_platform
```

## Redis Access

```bash
# Connect to Redis CLI
docker exec -it ai-agent-redis redis-cli
```

## Configuration

Key configuration in `application.properties`:

- **Database**: PostgreSQL on port 5433
- **Redis**: Port 6380
- **HTTP**: Port 8080
- **Mistral AI**: Configured via `MISTRAL_API_KEY` environment variable

## Features

- ✅ No-Code Agent Creation
- ✅ Business Integration via Custom Tools
- ✅ Document Intelligence with Vector Search
- ✅ Real-Time Monitoring
- ✅ Cost Management
- ✅ Multi-Tenancy with RBAC

## Documentation

- [Docker Setup Guide](DOCKER-SETUP.md) - Detailed Docker configuration and troubleshooting
- [Setup Guide](SETUP.md) - Complete setup verification and next steps

## Support

For issues or questions:
- Quarkus: https://quarkus.io/guides/
- LangChain4j: https://docs.langchain4j.dev/
- Mistral AI: https://docs.mistral.ai/

## License

[Your License Here]
