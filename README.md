# AI Agent Platform

A web-based platform that democratizes AI agent creation for non-technical users. Enables small businesses, individuals, and organizations to create, deploy, and manage custom AI agents without coding expertise.

## Quick Start

> **Note**: The application is currently in early development. Task 1 (project setup) is complete, but core features are still being implemented. See [Implementation Status](#implementation-status) below.

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

- **Web UI**: http://localhost:8080 (currently shows Quarkus welcome page)
- **Dev UI**: http://localhost:8080/q/dev
- **Health Check**: http://localhost:8080/q/health
- **Metrics**: http://localhost:8080/q/metrics

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
│   │   ├── domain/          # Entity models (✅ Implemented)
│   │   │   ├── Organization.java
│   │   │   ├── User.java
│   │   │   ├── Agent.java
│   │   │   ├── Tool.java
│   │   │   ├── AgentTool.java
│   │   │   ├── Conversation.java
│   │   │   ├── Message.java
│   │   │   ├── InteractionMetrics.java
│   │   │   ├── Document.java
│   │   │   └── DocumentEmbedding.java
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
│       ├── db/migration/    # Flyway migrations (✅ Implemented)
│       │   ├── V1__create_organizations_and_users.sql
│       │   ├── V2__create_agents_and_tools.sql
│       │   ├── V3__create_conversations_and_messages.sql
│       │   └── V4__create_documents_and_embeddings.sql
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

## Database Schema

The platform uses PostgreSQL with the following schema:

### Core Tables
- **organizations**: Multi-tenant organization management with usage limits and settings (JSONB)
- **users**: User accounts with email authentication, roles, and organization relationships
- **agents**: AI agent configurations with system prompts, status, and model settings
- **tools**: External API integrations with authentication configs and parameters (JSONB)
- **agent_tools**: Many-to-many junction table linking agents to their available tools

### Conversation Tables
- **conversations**: Chat sessions between users and agents with satisfaction tracking
- **messages**: Individual messages with role (USER/ASSISTANT/SYSTEM), content, and token counts
- **interaction_metrics**: Performance metrics including response time, token usage, and success rates

### Document Tables
- **documents**: Uploaded files with metadata, status tracking, and organization isolation
- **document_embeddings**: Vector embeddings (1536 dimensions) for semantic search using pgvector
  - Includes ivfflat index for efficient cosine similarity search
  - Supports chunked document processing with chunk_index

### Key Features
- UUID primary keys for all tables
- JSONB columns for flexible configuration storage
- Comprehensive indexing for query performance
- Foreign key constraints with CASCADE deletes where appropriate
- Multi-tenancy via organization_id filtering
- pgvector extension for semantic search capabilities

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

### Database
- **Type**: PostgreSQL 15+ with pgvector extension
- **Port**: 5433 (to avoid conflicts with default PostgreSQL)
- **Database**: `ai_agent_platform`
- **Connection Pool**: Max 16 connections
- **Migration**: Flyway enabled (baseline-on-migrate)

### Redis
- **Port**: 6380 (to avoid conflicts with default Redis)
- **Timeout**: 10 seconds
- **Usage**: Session management and chat memory (planned)

### HTTP Server
- **Port**: 8080
- **CORS**: Enabled for localhost:8080
- **WebSocket**: Configured with 'chat' subprotocol

### Mistral AI
- **API Key**: Set via `MISTRAL_API_KEY` environment variable
- **Chat Model**: `mistral-large-latest`
- **Temperature**: 0.7
- **Max Tokens**: 2000
- **Timeout**: 60 seconds
- **Embedding Model**: `mistral-embed`
- **Vector Dimension**: 1024 (for pgvector)

### Logging
- **Level**: INFO (DEBUG for com.platform package)
- **SQL Logging**: Enabled in dev mode
- **Console Colors**: Enabled in dev mode

## Implementation Status

### Completed
- ✅ **Task 1**: Project structure and core dependencies
  - Quarkus 3.17.7 with Maven configuration
  - LangChain4j 0.21.0 with Mistral AI integration
  - PostgreSQL with pgvector extension
  - Redis for caching
  - Qute templating engine
  - Tailwind CSS and Alpine.js (via CDN)
  - Application configuration and Docker setup

- ✅ **Task 2**: Database schema and entity models
  - Core entities: Organization, User, Agent, Tool, AgentTool
  - Conversation entities: Conversation, Message, InteractionMetrics
  - Document entities: Document, DocumentEmbedding with pgvector support
  - Complete Flyway migrations (V1-V4) with indexes and constraints
  - Multi-tenancy support with organization-level isolation

### In Progress
The following features are planned and documented in `.kiro/specs/ai-agent-platform/`:
- 🔄 Authentication and authorization (Task 3)
- 🔄 Dashboard layout and navigation (Task 4)
- 🔄 Agent wizard service and UI (Task 5)
- 🔄 LangChain4j AI services integration (Task 6)
- 🔄 Agent runtime service (Task 7)
- 🔄 Tool registry and execution (Task 8)
- 🔄 Vector store and document indexing (Task 9)
- 🔄 Monitoring and metrics (Task 10)
- 🔄 Usage tracking and cost management (Task 11)
- 🔄 REST API for programmatic access (Task 12)
- 🔄 Onboarding and help system (Task 13)
- 🔄 Error handling and user feedback (Task 14)
- 🔄 Security and GDPR compliance (Task 15)
- 🔄 Mobile responsive UI (Task 16)
- 🔄 Deployment and infrastructure (Task 17)
- 🔄 Performance optimization (Task 18)
- 🔄 Integration and E2E testing (Task 19)

### Planned Features
- No-Code Agent Creation via wizard interface
- Business Integration via custom tools
- Document Intelligence with vector search
- Real-Time Monitoring and metrics
- Cost Management with usage limits
- Multi-Tenancy with RBAC

## Documentation

- [Docker Setup Guide](DOCKER-SETUP.md) - Detailed Docker configuration and troubleshooting
- [Setup Guide](SETUP.md) - Complete setup verification and next steps
- [Requirements](.kiro/specs/ai-agent-platform/requirements.md) - Detailed product requirements
- [Implementation Tasks](.kiro/specs/ai-agent-platform/tasks.md) - Development roadmap and task breakdown
- [Product Overview](.kiro/steering/product.md) - Product vision and capabilities
- [Technology Stack](.kiro/steering/tech.md) - Technical architecture and dependencies
- [Project Structure](.kiro/steering/structure.md) - Code organization and conventions

## Next Steps for Development

To continue implementation, follow the task breakdown in `.kiro/specs/ai-agent-platform/tasks.md`:

1. **Task 3**: Implement authentication and authorization
   - Create JWT-based authentication service
   - Build login/registration UI with Qute
   - Implement RBAC with multi-tenancy

3. **Task 4**: Create dashboard layout
   - Build responsive base layout with Tailwind CSS
   - Implement navigation and routing
   - Create dashboard home page

4. **Tasks 5-19**: Continue with remaining features as documented

## Development Guidelines

- Follow the project structure defined in `.kiro/steering/structure.md`
- Use Panache for entity models and repositories
- Apply `@ApplicationScoped` for CDI services
- Prefix REST endpoints with `/api`
- Use Tailwind CSS for styling Qute templates
- Implement multi-tenancy at all layers (database, service, UI)
- Write tests for services and REST endpoints

## Support

For issues or questions:
- **Quarkus**: https://quarkus.io/guides/
- **LangChain4j**: https://docs.langchain4j.dev/
- **Mistral AI**: https://docs.mistral.ai/
- **Panache**: https://quarkus.io/guides/hibernate-orm-panache
- **Qute**: https://quarkus.io/guides/qute

## License

[Your License Here]
