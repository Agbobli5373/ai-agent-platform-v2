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

### 3. Generate JWT Keys (First Time Only)

The application requires RSA key pair for JWT token signing:

```bash
# Generate private key
openssl genrsa -out src/main/resources/privateKey.pem 2048

# Generate public key
openssl rsa -in src/main/resources/privateKey.pem -pubout -out src/main/resources/publicKey.pem
```

**Note**: These keys are for development only. In production, use secure key management (e.g., HashiCorp Vault, AWS KMS).

### 4. Run the Application

```bash
./mvnw quarkus:dev
```

### 5. Access the Application

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
- **Caching**: Redis 7 (session storage)
- **Security**: SmallRye JWT with BCrypt password hashing
- **Database Migration**: Flyway
- **WebSocket**: Quarkus WebSockets Next
- **Monitoring**: Micrometer with Prometheus
- **Validation**: Hibernate Validator

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
│   │   ├── service/         # Business logic (🔄 Partial)
│   │   │   ├── AuthenticationService.java (✅)
│   │   │   ├── AuthorizationService.java (✅)
│   │   │   └── RegistrationService.java (✅)
│   │   ├── repository/      # Data access (🔄 Partial)
│   │   │   └── UserRepository.java (✅)
│   │   ├── rest/            # REST API endpoints (🔄 Partial)
│   │   │   ├── AuthResource.java (✅)
│   │   │   ├── AuthPageResource.java (✅)
│   │   │   └── HomeResource.java (✅)
│   │   ├── websocket/       # WebSocket endpoints
│   │   ├── ai/              # AI service interfaces
│   │   ├── security/        # Security and auth (✅ Implemented)
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── PasswordHasher.java
│   │   │   ├── Role.java
│   │   │   ├── Permission.java
│   │   │   ├── RequiresPermission.java
│   │   │   ├── PermissionInterceptor.java
│   │   │   ├── RBACPolicy.java
│   │   │   └── dto/ (AuthenticationResponse, LoginRequest, etc.)
│   │   └── exception/       # Exception handlers (✅ Implemented)
│   │       ├── GlobalExceptionHandler.java
│   │       ├── AuthenticationException.java
│   │       ├── AuthorizationException.java
│   │       └── ValidationException.java
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
- **organizations**: Multi-tenant organization management with usage limits and settings
  - Uses `@JdbcTypeCode(SqlTypes.JSON)` for JSONB columns (usage_limit, settings)
  - Provides flexible configuration storage without schema changes
- **users**: User accounts with email authentication, roles, and organization relationships
- **agents**: AI agent configurations with system prompts, status, and model settings
- **tools**: External API integrations with authentication configs and parameters
  - Uses `@JdbcTypeCode(SqlTypes.JSON)` for flexible API configuration storage
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
- JSON/JSONB columns using Hibernate's `@JdbcTypeCode(SqlTypes.JSON)` for type-safe, portable configuration storage
- Comprehensive indexing for query performance
- Foreign key constraints with CASCADE deletes where appropriate
- Multi-tenancy via organization_id filtering
- pgvector extension for semantic search capabilities

## API Endpoints

### Authentication API

The platform provides REST endpoints for user authentication and authorization:

#### POST /api/auth/register
Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "organizationName": "My Organization"
}
```

**Response:** 201 Created with authentication tokens

#### POST /api/auth/login
Authenticate and receive access tokens.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "role": "USER",
    "organizationId": "uuid"
  }
}
```

#### POST /api/auth/logout
Invalidate the current session (requires authentication).

#### POST /api/auth/refresh
Refresh access token using refresh token (requires authentication with refresh token).

#### GET /api/auth/me
Get current authenticated user information (requires authentication).

**Response:**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "role": "USER",
  "organizationId": "uuid"
}
```

### Testing Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","organizationName":"Test Org"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}'

# Get current user (replace TOKEN with actual token)
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TOKEN"
```

### Dashboard API

The platform provides dashboard endpoints for monitoring and analytics:

#### GET /dashboard
Render the dashboard home page (HTML).

**Authentication:** Required (USER or ADMIN role)

**Response:** HTML page with dashboard interface

#### GET /dashboard/stats
Get dashboard statistics.

**Authentication:** Required (USER or ADMIN role)

**Response:**
```json
{
  "totalAgents": 0,
  "totalConversations": 0,
  "satisfactionScore": 0,
  "avgResponseTime": 0.0
}
```

#### GET /dashboard/activity
Get recent activity feed.

**Authentication:** Required (USER or ADMIN role)

**Response:**
```json
[
  {
    "id": "uuid",
    "type": "agent_created",
    "message": "New agent created successfully",
    "timestamp": "2024-11-17 10:30:00"
  }
]
```

**Activity Types:**
- `agent_created` - New agent was created
- `conversation` - New conversation started
- `tool_added` - New tool was registered
- `document_uploaded` - Document was uploaded

### Testing Dashboard

```bash
# Access dashboard home page (requires authentication)
# Login first, then navigate to: http://localhost:8080/dashboard

# Get dashboard stats (replace TOKEN with actual token)
curl -X GET http://localhost:8080/dashboard/stats \
  -H "Authorization: Bearer TOKEN"

# Get recent activity (replace TOKEN with actual token)
curl -X GET http://localhost:8080/dashboard/activity \
  -H "Authorization: Bearer TOKEN"
```

### Dashboard Features

The dashboard provides a modern, responsive interface with the following features:

**Layout Components:**
- Collapsible sidebar navigation (desktop) - toggles between expanded and collapsed states
- Mobile slide-out sidebar with overlay backdrop
- Top navigation bar with breadcrumbs, notifications, and user profile
- User profile dropdown with organization switcher
- Dark mode toggle (optional feature)
- Responsive footer with documentation links

**Home Page:**
- Quick stats cards showing:
  - Total agents with monthly growth indicator
  - Total conversations with weekly comparison
  - Customer satisfaction score with improvement percentage
  - Average response time with performance delta
- Recent activity feed with type-based color coding:
  - `agent_created` (blue) - New agent created
  - `conversation` (green) - New conversation started
  - `tool_added` (purple) - Tool registered
  - `document_uploaded` (orange) - Document uploaded
- Quick action buttons for common tasks:
  - Create Agent - Launch wizard
  - Add Tool - Register API integration
  - Upload Document - Add to knowledge base
  - View Docs - Access documentation
- Welcome banner for new users (shown when no agents exist)

**Interactive Features:**
- Real-time data loading via Alpine.js
- Smooth transitions and animations
- Touch-optimized for mobile devices
- Persistent sidebar state (saved to localStorage)
- Active page highlighting in navigation
- Notification dropdown with recent alerts

**Technical Implementation:**
- Server-side rendering with Qute templates
- Tailwind CSS for responsive styling
- Alpine.js for client-side interactivity
- JWT-based authentication required for all dashboard pages
- Organization-level data isolation enforced

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

# Check active sessions
docker exec -it ai-agent-redis redis-cli KEYS "session:*"
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
- **Usage**: Session management (active), chat memory (planned)

### Security & JWT
- **Issuer**: https://ai-agent-platform.com
- **Access Token Duration**: 24 hours
- **Refresh Token Duration**: 168 hours (7 days)
- **Key Files**: privateKey.pem (signing), publicKey.pem (verification)
- **Session Storage**: Redis with 24-hour expiration
- **Password Hashing**: BCrypt with strength 12

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

- ✅ **Task 2**: Database schema and entity models (COMPLETE)
  - ✅ Task 2.1: Core entities (Organization, User, Agent, Tool, AgentTool)
  - ✅ Task 2.2: Conversation entities (Conversation, Message, InteractionMetrics)
  - ✅ Task 2.3: Document entities (Document, DocumentEmbedding with pgvector)
  - ✅ Task 2.4: Complete Flyway migrations (V1-V4) with indexes and constraints
  - Multi-tenancy support with organization-level isolation
  - All 10 entity models implemented with Panache
  - Vector similarity search configured with pgvector

### In Progress
- 🔄 **Task 3**: Authentication and authorization service (IN PROGRESS)
  - ✅ Task 3.1: Authentication service with JWT support
    - AuthenticationService with login and session management
    - JwtTokenProvider for token generation and validation
    - PasswordHasher utility using BCrypt
    - UserRepository with Panache queries
    - Redis-based session storage
  - ✅ Task 3.2: Role-based access control (PARTIAL)
    - RBAC policy engine with permission checking
    - Role definitions (ADMIN, USER, VIEWER)
    - Permission enum and RequiresPermission annotation
    - PermissionInterceptor for method-level authorization
    - AuthorizationService for context-aware permission checks
    - Organization-level multi-tenancy isolation
  - ✅ Task 3.3: Authentication REST endpoints
    - POST /api/auth/login - User authentication
    - POST /api/auth/logout - Session invalidation
    - POST /api/auth/refresh - Token refresh
    - POST /api/auth/register - User registration
    - GET /api/auth/me - Current user info
    - Secured with @PermitAll and @Authenticated annotations
  - ⏳ Task 3.4: Authentication and registration UI with Qute (PENDING)
  - ⏳ Task 3.5: Authentication service tests (PENDING)

- ✅ **Task 4**: Modern dashboard layout and navigation (COMPLETE)
  - ✅ Task 4.1: Base dashboard layout template
    - Responsive dashboard layout with Tailwind CSS
    - Collapsible sidebar navigation (desktop) with toggle
    - Mobile slide-out sidebar with overlay
    - Top navigation bar with user profile dropdown
    - Breadcrumb navigation component
    - Dark mode toggle (optional feature)
    - Footer with documentation links
  - ✅ Task 4.2: Dashboard home page
    - Dashboard home template with overview cards
    - Quick stats cards (agents, conversations, satisfaction, response time)
    - Recent activity feed with type-based icons
    - Quick action buttons (Create Agent, Add Tool, Upload Document, View Docs)
    - Welcome message for new users
    - Alpine.js integration for dynamic data loading
  - ✅ Task 4.3: Navigation and routing
    - Navigation menu items (Dashboard, Agents, Tools, Documents, Settings)
    - Active page highlighting based on current path
    - User profile dropdown with logout functionality
    - Organization switcher component
    - Notification bell with badge indicator
    - Mobile-responsive navigation

The following features are planned and documented in `.kiro/specs/ai-agent-platform/`:
- 🔄 Authentication and authorization (Task 3) - Partially complete
- ✅ Dashboard layout and navigation (Task 4) - Complete
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

## Security Implementation

### Authentication Flow
1. User registers via `/api/auth/register` with email, password, and organization name
2. Password is hashed using BCrypt (strength 12) before storage
3. User logs in via `/api/auth/login` with credentials
4. System validates credentials and generates JWT access token (24h) and refresh token (7d)
5. Session is stored in Redis with 24-hour expiration
6. Access token must be included in `Authorization: Bearer <token>` header for protected endpoints
7. User can refresh token via `/api/auth/refresh` before expiration
8. User can logout via `/api/auth/logout` to invalidate session

### Authorization
- **Roles**: ADMIN, USER, VIEWER
- **Permissions**: Granular permissions (e.g., AGENT_CREATE, AGENT_DELETE, TOOL_MANAGE)
- **RBAC Policy**: Role-based access control with permission checking
- **Multi-Tenancy**: Organization-level isolation enforced at all layers
- **Method Security**: `@RequiresPermission` annotation for method-level authorization
- **Endpoint Security**: `@Authenticated` and `@PermitAll` annotations for REST endpoints

### Key Security Classes
- **JwtTokenProvider**: Generates and validates JWT tokens
- **PasswordHasher**: BCrypt password hashing and verification
- **AuthenticationService**: Login, logout, session management
- **AuthorizationService**: Permission checking and context management
- **PermissionInterceptor**: CDI interceptor for method-level authorization
- **RBACPolicy**: Role-to-permission mapping

### Key Service Classes
- **DashboardService**: Dashboard statistics and activity feed
  - `getDashboardStats()`: Returns agent, conversation, satisfaction, and response time metrics
  - `getRecentActivity()`: Returns recent platform activity with timestamps
  - Currently returns mock data; will be implemented with database queries in future tasks

## Next Steps for Development

To continue implementation, follow the task breakdown in `.kiro/specs/ai-agent-platform/tasks.md`:

1. **Task 3.4**: Build authentication and registration UI with Qute
   - Create login page template
   - Create registration page template
   - Implement form validation and error display
   - Add responsive design for mobile

2. **Task 3.5**: Write authentication service tests
   - Unit tests for JWT token generation
   - Integration tests for login flow
   - Test RBAC permission checking

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
- Use `@RequiresPermission` for method-level authorization
- Use `@Authenticated` or `@PermitAll` for REST endpoint security
- Always validate user's organization context for multi-tenancy
- Hash passwords with BCrypt before storage (never store plain text)
- Include JWT token in `Authorization: Bearer <token>` header for protected endpoints

## Troubleshooting

### Authentication Issues

**Problem**: "Invalid email or password" error
- Verify user exists in database: `SELECT * FROM users WHERE email = 'your-email';`
- Ensure password was hashed correctly during registration
- Check Redis connection for session storage

**Problem**: "JWT token validation failed"
- Ensure `privateKey.pem` and `publicKey.pem` exist in `src/main/resources/`
- Verify token hasn't expired (24h for access token)
- Check token is included in `Authorization: Bearer <token>` header
- Use refresh token endpoint if access token expired

**Problem**: "Insufficient permissions" error
- Verify user role in database: `SELECT role FROM users WHERE id = 'user-id';`
- Check permission requirements in `@RequiresPermission` annotation
- Ensure user belongs to correct organization for multi-tenancy

**Problem**: Redis connection failed
- Verify Redis is running: `docker ps | grep redis`
- Check Redis port (6380): `docker exec -it ai-agent-redis redis-cli ping`
- Restart Redis: `docker-compose restart redis`

### Database Issues

**Problem**: Flyway migration failed
- Check migration scripts in `src/main/resources/db/migration/`
- Verify PostgreSQL is running and accessible on port 5433
- Check database exists: `docker exec -it ai-agent-postgres psql -U postgres -l`
- Reset database (⚠️ deletes all data): `docker-compose down -v && docker-compose up -d`

## Support

For issues or questions:
- **Quarkus**: https://quarkus.io/guides/
- **LangChain4j**: https://docs.langchain4j.dev/
- **Mistral AI**: https://docs.mistral.ai/
- **Panache**: https://quarkus.io/guides/hibernate-orm-panache
- **Qute**: https://quarkus.io/guides/qute
- **SmallRye JWT**: https://quarkus.io/guides/security-jwt

## License

[Your License Here]
