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
│   │   ├── ai/              # AI service interfaces (🔄 Partial)
│   │   │   ├── MistralAIConfig.java (✅)
│   │   │   ├── RedisChatMemory.java (✅)
│   │   │   └── EmbeddingService.java (✅)
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
│       │   ├── V4__create_documents_and_embeddings.sql
│       │   └── V5__add_created_at_to_conversations.sql
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
  - Includes `created_at` timestamp for record creation tracking (separate from `started_at` for conversation start)
  - Indexed on `status` for efficient filtering by conversation state
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

**Authentication:** Public access (authentication handled client-side via JavaScript)

**Response:** HTML page with dashboard interface

**Note:** The endpoint returns a static template with placeholder data. Actual user data is loaded dynamically via JavaScript using the JWT token stored in localStorage. This approach enables:
- Faster initial page load
- Client-side authentication handling
- Better separation of concerns between server-side rendering and data fetching

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

### Wizard API

The platform provides REST endpoints for the agent creation wizard:

#### POST /api/wizard/session
Initialize a new wizard session.

**Authentication:** Required (USER or ADMIN role)

**Response:**
```json
{
  "sessionId": "uuid",
  "currentStep": "PURPOSE"
}
```

#### GET /api/wizard/session/{sessionId}
Get current wizard session state.

**Authentication:** Required (must own the session)

**Response:**
```json
{
  "sessionId": "uuid",
  "userId": "uuid",
  "currentStep": "PURPOSE",
  "stepData": {
    "PURPOSE": { "name": "Customer Support Bot", "description": "..." },
    "PROMPT": { "systemPrompt": "..." }
  },
  "createdAt": "2024-11-17T10:30:00"
}
```

#### PUT /api/wizard/session/{sessionId}/step
Save wizard step data.

**Authentication:** Required (must own the session)

**Request Body:**
```json
{
  "step": "PURPOSE",
  "data": {
    "name": "Customer Support Bot",
    "description": "Helps customers with common questions"
  }
}
```

**Response:** 200 OK with success message

#### POST /api/wizard/validate
Validate agent configuration.

**Authentication:** Required

**Request Body:**
```json
{
  "name": "Customer Support Bot",
  "description": "Helps customers",
  "systemPrompt": "You are a helpful customer support assistant...",
  "temperature": 0.7,
  "maxTokens": 2000,
  "toolIds": ["uuid1", "uuid2"]
}
```

**Response:**
```json
{
  "valid": true,
  "errors": []
}
```

Or if invalid:
```json
{
  "valid": false,
  "errors": ["Agent name is required", "System prompt must be at least 10 characters"]
}
```

**Validation Rules:**
- **Name**: Required, 3-255 characters (trimmed)
- **Description**: Optional, max 1000 characters (trimmed)
- **System Prompt**: Required, 10-10000 characters (trimmed)
- **Model Name**: Required (non-blank)
- **Tool IDs**: Optional array of UUIDs

**Implementation Details:**
- Validation logic in `AgentWizardService.validateConfiguration()`
- Returns `ValidationResult` with `valid` boolean and `errors` array
- Used by both `/validate` endpoint and during deployment
- Prevents invalid agents from being created

#### POST /api/wizard/preview
Preview agent with test prompt.

**Authentication:** Required

**Request Body:**
```json
{
  "config": {
    "name": "Customer Support Bot",
    "systemPrompt": "You are a helpful assistant...",
    "temperature": 0.7,
    "maxTokens": 2000
  },
  "testPrompt": "What are your business hours?"
}
```

**Response:**
```json
{
  "response": "Our business hours are Monday-Friday, 9 AM to 5 PM EST.",
  "tokensUsed": 45,
  "responseTime": 1.2
}
```

#### POST /api/wizard/deploy
Deploy agent to production.

**Authentication:** Required

**Request Body:**
```json
{
  "sessionId": "uuid",
  "config": {
    "name": "Customer Support Bot",
    "description": "Helps customers with common questions",
    "systemPrompt": "You are a helpful customer support assistant...",
    "temperature": 0.7,
    "maxTokens": 2000,
    "toolIds": ["uuid1", "uuid2"]
  }
}
```

**Response:** 201 Created
```json
{
  "agentId": "uuid",
  "name": "Customer Support Bot",
  "status": "ACTIVE"
}
```

#### DELETE /api/wizard/session/{sessionId}
Delete wizard session.

**Authentication:** Required (must own the session)

**Response:** 200 OK with success message

### Testing Wizard API

```bash
# Initialize wizard session
curl -X POST http://localhost:8080/api/wizard/session \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json"

# Save step data (replace SESSION_ID)
curl -X PUT http://localhost:8080/api/wizard/session/SESSION_ID/step \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "step": "PURPOSE",
    "data": {
      "name": "Customer Support Bot",
      "description": "Helps customers with common questions"
    }
  }'

# Validate configuration
curl -X POST http://localhost:8080/api/wizard/validate \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Support Bot",
    "systemPrompt": "You are a helpful assistant...",
    "temperature": 0.7,
    "maxTokens": 2000
  }'

# Preview agent
curl -X POST http://localhost:8080/api/wizard/preview \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "config": {
      "name": "Customer Support Bot",
      "systemPrompt": "You are a helpful assistant...",
      "temperature": 0.7,
      "maxTokens": 2000
    },
    "testPrompt": "What are your business hours?"
  }'

# Deploy agent
curl -X POST http://localhost:8080/api/wizard/deploy \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "SESSION_ID",
    "config": {
      "name": "Customer Support Bot",
      "description": "Helps customers",
      "systemPrompt": "You are a helpful assistant...",
      "temperature": 0.7,
      "maxTokens": 2000
    }
  }'
```

### Agent Runtime API

The platform provides WebSocket and service APIs for real-time agent interactions:

#### WebSocket: /ws/agent/{agentId}/chat
Real-time chat with streaming responses.

**Connection:** WebSocket connection to `/ws/agent/{agentId}/chat`

**Message Format (JSON):**
```json
{
  "message": "What are your business hours?",
  "userId": "uuid",
  "conversationId": "uuid"
}
```

**Response:** Streaming text tokens as they are generated by the AI

**Features:**
- Real-time streaming responses
- Automatic conversation tracking per connection
- Error handling with descriptive messages
- Connection lifecycle management (onOpen, onMessage, onClose, onError)

**JavaScript Example:**
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/agent/AGENT_ID/chat');

ws.onopen = () => {
  console.log('Connected to agent');
  ws.send(JSON.stringify({
    message: 'Hello, how can you help me?',
    userId: 'USER_ID',
    conversationId: null  // null for new conversation
  }));
};

ws.onmessage = (event) => {
  console.log('Received:', event.data);
  // Append token to UI
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};

ws.onclose = () => {
  console.log('Disconnected from agent');
};
```

#### AgentRuntimeService API

**Service Methods:**

**processMessage()** - Synchronous message processing
```java
CompletionStage<AgentResponse> processMessage(
    UUID agentId,
    String userMessage,
    UUID conversationId,  // null for new conversation
    UUID userId
)
```

**Response:**
```java
class AgentResponse {
    UUID conversationId;
    String content;
    UUID messageId;
    LocalDateTime timestamp;
}
```

**streamResponse()** - Streaming message processing
```java
Multi<String> streamResponse(
    UUID agentId,
    String userMessage,
    UUID conversationId,
    UUID userId
)
```

**Features:**
- Conversation context management (last 10 messages)
- Automatic message persistence
- Agent status validation (must be ACTIVE)
- Support for both new and existing conversations

#### ToolExecutionOrchestrator API

**executeToolChain()** - Execute multiple tools with orchestration
```java
CompletionStage<ToolChainResult> executeToolChain(
    Agent agent,
    List<String> toolIds,
    Map<String, Object> context
)
```

**Features:**
- Parallel tool execution with CompletableFuture
- Retry logic with exponential backoff (3 retries)
- Timeout handling (10 seconds default)
- Result aggregation from all tools
- Support for REST_API, FUNCTION, and DATABASE tool types

**Response:**
```java
class ToolChainResult {
    boolean success;
    String summary;
    Map<String, Object> results;  // toolId -> result mapping
}
```

### Tool Registry API

The platform provides REST endpoints for managing external API integrations (tools) that agents can use:

#### POST /api/tools
Register a new tool.

**Authentication:** Required (USER or ADMIN role)

**Request Body:**
```json
{
  "name": "Weather API",
  "description": "Get weather information for any city",
  "type": "REST_API",
  "endpoint": "https://api.openweathermap.org/data/2.5/weather",
  "authConfig": {
    "type": "API_KEY",
    "apiKey": "your-api-key-here"
  },
  "parameters": {
    "city": {
      "name": "city",
      "type": "string",
      "description": "City name",
      "required": true
    }
  }
}
```

**Response:** 201 Created with tool details

**Authentication Types:**
- `API_KEY` - API key authentication (Bearer token or X-API-Key header)
- `BASIC_AUTH` - HTTP Basic authentication (username/password)
- `OAUTH2` - OAuth2 Bearer token
- `NONE` - No authentication required

#### GET /api/tools
List all tools for the current organization.

**Authentication:** Required (USER, ADMIN, or VIEWER role)

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Weather API",
    "description": "Get weather information",
    "type": "REST_API",
    "endpoint": "https://api.openweathermap.org/data/2.5/weather",
    "createdAt": "2024-11-18T10:30:00"
  }
]
```

#### GET /api/tools/{toolId}
Get a specific tool by ID.

**Authentication:** Required (USER, ADMIN, or VIEWER role)

**Response:** Tool details (same format as POST response)

#### PUT /api/tools/{toolId}
Update an existing tool.

**Authentication:** Required (USER or ADMIN role)

**Request Body:** Same format as POST /api/tools

**Response:** 200 OK with updated tool details

#### DELETE /api/tools/{toolId}
Delete a tool.

**Authentication:** Required (ADMIN role only)

**Response:** 200 OK with success message

#### POST /api/tools/{toolId}/validate
Validate tool connection and authentication.

**Authentication:** Required (USER or ADMIN role)

**Response:**
```json
{
  "valid": true,
  "message": "Connection successful",
  "responseTimeMs": 245,
  "statusCode": 200
}
```

Or if validation fails:
```json
{
  "valid": false,
  "message": "Connection error: ...",
  "errorDetails": "..."
}
```

#### POST /api/tools/{toolId}/execute
Execute a tool with given parameters.

**Authentication:** Required (USER or ADMIN role)

**Request Body:**
```json
{
  "city": "London",
  "units": "metric"
}
```

**Response:**
```json
{
  "success": true,
  "result": {
    "temperature": 15,
    "conditions": "Rainy"
  },
  "executionTimeMs": 1234,
  "metadata": {
    "statusCode": 200,
    "toolId": "uuid",
    "toolName": "Weather API"
  }
}
```

**Features:**
- Timeout protection (10 seconds)
- Automatic retry with exponential backoff (3 attempts)
- Circuit breaker pattern (opens after 50% failure rate)
- Support for GET and POST HTTP methods
- Secure credential storage

#### GET /api/tools/type/{type}
Get tools filtered by type.

**Authentication:** Required (USER, ADMIN, or VIEWER role)

**Tool Types:**
- `REST_API` - External REST API integration
- `FUNCTION` - Custom function execution
- `DATABASE` - Database query tool

#### GET /api/tools/search?q={query}
Search tools by name or description.

**Authentication:** Required (USER, ADMIN, or VIEWER role)

**Response:** Array of matching tools

### Tool Management UI

The platform provides web pages for managing tools:

#### GET /tools
Tool list page with all organization tools.

**Authentication:** Public access (data loaded client-side with JWT)

**Features:**
- List all tools with name, description, type, and endpoint
- Test button to validate tool connection
- Edit and delete actions
- Create new tool button
- Responsive design with Tailwind CSS

#### GET /tools/create
Tool creation form page.

**Authentication:** Public access (form submission requires JWT)

**Features:**
- Form fields for tool configuration
- Authentication type selector (API_KEY, BASIC_AUTH, OAUTH2, NONE)
- Parameter definition interface
- Real-time validation
- Submit creates tool via POST /api/tools

#### GET /tools/{toolId}/edit
Tool editing form page.

**Authentication:** Public access (data loaded and saved with JWT)

**Features:**
- Pre-populated form with existing tool data
- Same fields as creation form
- Update button saves via PUT /api/tools/{toolId}

**Technical Implementation:**
- Server-side rendering with Qute templates
- Qute template variable `{toolId}` passed directly to Alpine.js component as parameter
- Client-side data loading via JavaScript fetch with JWT authentication
- Alpine.js for form interactivity
- Automatic redirect to login if token missing/expired

**Implementation Pattern:**
```html
<!-- Pass Qute variable to Alpine.js component -->
<div x-data="toolEditForm('{toolId}')" x-init="init()">
```

```javascript
// Accept toolId as parameter and store in component state
function toolEditForm(toolId) {
    return {
        toolId: toolId,  // Store for reuse in API calls
        async init() {
            const response = await fetch('/api/tools/' + this.toolId, {
                headers: { 'Authorization': 'Bearer ' + token }
            });
            // Load tool data...
        }
    }
}
```

**Key Pattern:** Pass server-side template variables directly to Alpine.js components rather than parsing URLs client-side. This ensures data consistency and simplifies the code.

### Testing Tool Registry API

```bash
# Register a new tool
curl -X POST http://localhost:8080/api/tools \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Weather API",
    "description": "Get weather information",
    "type": "REST_API",
    "endpoint": "https://api.openweathermap.org/data/2.5/weather",
    "authConfig": {
      "type": "API_KEY",
      "apiKey": "your-api-key"
    }
  }'

# List all tools
curl -X GET http://localhost:8080/api/tools \
  -H "Authorization: Bearer TOKEN"

# Validate tool connection
curl -X POST http://localhost:8080/api/tools/TOOL_ID/validate \
  -H "Authorization: Bearer TOKEN"

# Execute tool
curl -X POST http://localhost:8080/api/tools/TOOL_ID/execute \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"city": "London", "units": "metric"}'

# Delete tool
curl -X DELETE http://localhost:8080/api/tools/TOOL_ID \
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
- Server-side rendering with Qute templates (static HTML with placeholder data)
- Client-side data loading via JavaScript fetch API with JWT authentication
- Tailwind CSS for responsive styling
- Alpine.js for client-side interactivity
- JWT token stored in localStorage for API authentication
- Organization-level data isolation enforced at API layer
- Automatic redirect to login page if token is missing or expired

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

# Check chat memory
docker exec -it ai-agent-redis redis-cli KEYS "chat:memory:*"

# View conversation history (replace CONVERSATION_ID)
docker exec -it ai-agent-redis redis-cli GET "chat:memory:CONVERSATION_ID"
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
- **Usage**: Session management (active), chat memory (active)
- **Chat Memory**: Stores conversation history with 24-hour TTL
  - Key format: `chat:memory:{conversationId}`
  - Message format: `{role}: {content}` (newline-separated)

### Security & JWT
- **Issuer**: https://ai-agent-platform.com
- **Access Token Duration**: 24 hours
- **Refresh Token Duration**: 168 hours (7 days)
- **Key Files**: privateKey.pem (signing), publicKey.pem (verification)
- **Session Storage**: Redis with 24-hour expiration
- **Password Hashing**: BCrypt with strength 12
- **JWT Claims Access**: Uses `JsonWebToken` interface for reading claims
  - User ID: `jwt.getSubject()` (stored in "sub" claim)
  - Organization ID: `jwt.getClaim("organizationId")` (custom claim)
  - Implemented in `AuthorizationService` and `SecurityContext`

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

## Troubleshooting

### JWT Authentication Issues

**Problem:** "User ID not found in token" or "Insufficient permissions to access this resource"

**Solution:** The application uses the `JsonWebToken` interface to read JWT claims. Ensure your code uses:
- `jwt.getSubject()` for user ID (not `securityIdentity.getAttribute("sub")`)
- `jwt.getClaim("organizationId")` for organization ID (not `securityIdentity.getAttribute("organizationId")`)

**Example:**
```java
@Inject
JsonWebToken jwt;

public UUID getCurrentUserId() {
    String userId = jwt.getSubject();  // ✅ Correct
    return UUID.fromString(userId);
}

public UUID getCurrentOrganizationId() {
    String orgId = jwt.getClaim("organizationId");  // ✅ Correct
    return UUID.fromString(orgId);
}
```

**Reference:** See `AuthorizationService` and `SecurityContext` for proper implementation.

### Database Connection Issues

**Problem:** Cannot connect to PostgreSQL or Redis

**Solution:** Ensure Docker containers are running:
```bash
docker-compose ps
docker-compose logs postgres
docker-compose logs redis
```

Check ports are not in use:
- PostgreSQL: 5433
- Redis: 6380

### Quarkus Dev Mode Not Hot-Reloading

**Problem:** Changes not reflected after saving files

**Solution:** 
1. Check console for compilation errors
2. Restart dev mode: `./mvnw quarkus:dev`
3. Clear target directory: `./mvnw clean`

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
  - ✅ Task 2.4: Complete Flyway migrations (V1-V5) with indexes and constraints
    - V5: Added `created_at` column to conversations table and `status` index
  - Multi-tenancy support with organization-level isolation
  - All 10 entity models implemented with Panache
  - Vector similarity search configured with pgvector

- ✅ **Task 3**: Authentication and authorization service (COMPLETE)
  - ✅ Task 3.1: Authentication service with JWT support
    - AuthenticationService with login and session management
    - JwtTokenProvider for token generation and validation
    - PasswordHasher utility using BCrypt
    - UserRepository with Panache queries
    - Redis-based session storage
  - ✅ Task 3.2: Role-based access control (COMPLETE - some items marked incomplete in tasks.md)
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
  - ✅ Task 3.4: Authentication and registration UI with Qute
    - Login page template with email and password fields
    - Registration page template with user details form
    - Form validation and error display
    - Password strength indicator component
    - "Forgot password" page template
    - Responsive design for mobile login/registration
    - Success/error message display
  - ⏳ Task 3.5: Authentication service tests (PENDING - optional test task)

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

### In Progress
- 🔄 **Task 5**: Agent wizard service and UI (IN PROGRESS)
  - ✅ Task 5.1: Agent wizard service
    - AgentWizardService with session management
    - Wizard step validation logic
    - Agent configuration builder
    - Agent deployment method that persists to database
    - Preview functionality with test prompt execution
  - ✅ Task 5.2: Qute templates for wizard interface
    - Wizard layout template with step navigation and Tailwind styling
    - Step 1 template for agent purpose and name input
    - Step 2 template for system prompt configuration with textarea
    - Step 3 template for tool selection with checkboxes
    - Step 4 template for preview and deployment with test chat
    - Progress indicator showing current step
    - Modern styling with Tailwind CSS
  - ✅ Task 5.3: Wizard REST endpoints
    - POST /api/wizard/session - Initialize wizard session
    - GET /api/wizard/session/{sessionId} - Get session state
    - PUT /api/wizard/session/{sessionId}/step - Save step data
    - POST /api/wizard/validate - Validate configuration
    - POST /api/wizard/preview - Preview agent with test prompt
    - POST /api/wizard/deploy - Deploy agent
    - DELETE /api/wizard/session/{sessionId} - Delete session
    - All endpoints secured with JWT authentication
    - Session ownership verification for security
  - ✅ Task 5.4: Wizard validation logic
    - Agent name validation (required, 3-255 characters)
    - Description validation (optional, max 1000 characters)
    - System prompt validation (required, 10-10000 characters)
    - Model name validation (required)
    - Configuration completeness checking
    - Integrated with validateConfiguration() endpoint
  - ⏳ Task 5.5: Wizard service tests (PENDING - optional test task)

- ✅ **Task 6**: Tool registry and execution service (COMPLETE)
  - ✅ Task 6.1: Tool registry service
    - ToolRegistryService with CRUD operations
    - Tool validation and connection testing
    - Organization-level tool isolation
    - Support for multiple authentication types (API_KEY, BASIC_AUTH, OAUTH2, NONE)
  - ✅ Task 6.2: Tool executor with fault tolerance
    - ToolExecutor with HTTP client integration
    - Timeout protection (10 seconds)
    - Automatic retry with exponential backoff (3 attempts, 1s delay, 500ms jitter)
    - Circuit breaker pattern (opens after 50% failure rate over 10 requests)
    - Support for GET and POST HTTP methods
    - Secure authentication header injection
  - ✅ Task 6.3: Tool REST endpoints
    - POST /api/tools - Register new tool
    - GET /api/tools - List organization tools
    - GET /api/tools/{toolId} - Get tool details
    - PUT /api/tools/{toolId} - Update tool
    - DELETE /api/tools/{toolId} - Delete tool (ADMIN only)
    - POST /api/tools/{toolId}/validate - Test connection
    - POST /api/tools/{toolId}/execute - Execute tool
    - GET /api/tools/type/{type} - Filter by type
    - GET /api/tools/search?q={query} - Search tools
    - All endpoints secured with JWT and role-based access control
  - ✅ Task 6.4: Tool management UI
    - GET /tools - Tool list page with test/edit/delete actions
    - GET /tools/create - Tool creation form
    - GET /tools/{toolId}/edit - Tool editing form
    - Server-side rendering with Qute templates
    - Client-side data loading with JWT authentication
    - Alpine.js for interactive forms
    - Responsive design with Tailwind CSS
  - ✅ Task 6.5: LangChain4j tool integration
    - LangChainToolProvider with @Tool annotations
    - executeTool() method for agent tool execution
    - listAvailableTools() for tool discovery
    - getToolInfo() for tool metadata
    - Integration with AgentAIService
  - ⏳ Task 6.6: Tool execution tests (PENDING - optional test task)

- 🔄 **Task 7**: Agent runtime service (IN PROGRESS)
  - ✅ Task 7.1: Agent runtime service core
    - AgentRuntimeService with message processing
    - Conversation context management (last 10 messages)
    - Agent configuration loader from database
    - Response streaming with Multi<String>
    - Synchronous and asynchronous message processing
  - ✅ Task 7.2: WebSocket endpoint for real-time chat
    - WebSocket endpoint at /ws/agent/{agentId}/chat
    - OnOpen handler for connection initialization
    - OnMessage handler for user input processing
    - Streaming response handler to client
    - Error handling and connection management
    - Conversation ID tracking per connection
  - ✅ Task 7.3: Agent execution orchestration
    - ToolExecutionOrchestrator with tool chain execution
    - Retry logic with exponential backoff (3 retries)
    - Timeout handling for long-running operations (10s default)
    - Response aggregation from multiple tool calls
    - Support for REST_API, FUNCTION, and DATABASE tool types
  - ⏳ Task 7.4: Runtime service tests (PARTIAL - basic tests implemented)

The following features are planned and documented in `.kiro/specs/ai-agent-platform/`:
- ✅ Authentication and authorization (Task 3) - Complete
- ✅ Dashboard layout and navigation (Task 4) - Complete
- 🔄 Agent wizard service and UI (Task 5) - In Progress (4/5 subtasks complete)
- ⏳ LangChain4j AI services integration (Task 6)
- 🔄 Agent runtime service (Task 7) - In Progress (3/4 subtasks complete)
- ✅ Tool registry and execution (Task 8) - Complete (5/5 subtasks complete, tests pending)
- ⏳ Vector store and document indexing (Task 9)
- ⏳ Monitoring and metrics (Task 10)
- ⏳ Usage tracking and cost management (Task 11)
- ⏳ REST API for programmatic access (Task 12)
- ⏳ Onboarding and help system (Task 13)
- ⏳ Error handling and user feedback (Task 14)
- ⏳ Security and GDPR compliance (Task 15)
- ⏳ Mobile responsive UI (Task 16)
- ⏳ Deployment and infrastructure (Task 17)
- ⏳ Performance optimization (Task 18)
- ⏳ Integration and E2E testing (Task 19)

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
- [Dashboard Architecture](DASHBOARD_ARCHITECTURE.md) - Dashboard rendering and data loading architecture
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

### Key AI Service Classes
- **AgentAIService**: LangChain4j AI service interface for agent interactions
  - `chat()`: Synchronous chat with custom system prompt
  - `chatStream()`: Streaming chat for real-time responses
  - `chatWithContext()`: Chat with conversation history context
  - Registered with `@RegisterAiService` for automatic LangChain4j integration
  - Uses Mistral AI as the LLM provider
- **MistralAIConfig**: Configuration for Mistral AI models
  - Produces `defaultChatModel` and `streamingChatModel` beans
  - Configurable temperature, max tokens, and timeout
- **EmbeddingService**: Document embedding generation
  - Uses Mistral AI embedding model (mistral-embed)
  - 1024-dimensional vectors for pgvector
- **RedisChatMemory**: Conversation memory with 24-hour TTL

### Key Service Classes
- **DashboardService**: Dashboard statistics and activity feed
  - `getDashboardStats()`: Returns agent, conversation, satisfaction, and response time metrics
  - `getRecentActivity()`: Returns recent platform activity with timestamps
  - Currently returns mock data; will be implemented with database queries in future tasks
- **AgentWizardService**: Agent creation wizard management
  - `createSession()`: Initialize new wizard session with Redis storage (2-hour expiration)
  - `getSession()`: Retrieve wizard session state from Redis
  - `saveStep()`: Save wizard step data to session
  - `validateConfiguration()`: Comprehensive validation of agent configuration
    - Name validation: Required, 3-255 characters
    - Description validation: Optional, max 1000 characters
    - System prompt validation: Required, 10-10000 characters
    - Model name validation: Required, non-blank
    - Returns `ValidationResult` with errors array
  - `previewAgent()`: Test agent with sample prompt (mock implementation)
  - `deployAgent()`: Deploy agent to production database with validation
  - `deleteSession()`: Clean up wizard session from Redis
- **AgentRuntimeService**: Agent execution and conversation management
  - `processMessage()`: Synchronous message processing with conversation context
  - `streamResponse()`: Streaming message processing for real-time chat
  - `loadAgentConfiguration()`: Load agent from database with validation
  - `getOrCreateConversation()`: Manage conversation lifecycle
  - `buildConversationContext()`: Build context from last 10 messages
  - `executeTools()`: Orchestrate tool execution for agents
- **ToolExecutionOrchestrator**: Tool execution with fault tolerance
  - `executeToolChain()`: Execute multiple tools in parallel
  - `executeToolWithRetry()`: Execute single tool with retry logic (3 attempts)
  - Exponential backoff with jitter for retries
  - Timeout handling (10 seconds default)
  - Support for REST_API, FUNCTION, and DATABASE tool types
  - Result aggregation and error handling
- **RedisChatMemory**: Redis-based conversation memory management
  - `storeMessage()`: Store conversation messages with role and content
  - `getMessages()`: Retrieve all messages for a conversation
  - `getConversationHistory()`: Get formatted conversation history as string
  - `clearMemory()`: Delete all messages for a conversation
  - `updateTTL()`: Update time-to-live for conversation data
  - `hasMemory()`: Check if conversation has stored messages
  - Storage format: `chat:memory:{conversationId}` with 24-hour TTL
  - Messages stored as newline-separated strings: `{role}: {content}`

### Dashboard Architecture

The dashboard follows a hybrid rendering approach:

1. **Server-Side**: Qute template renders static HTML structure with placeholder data
2. **Client-Side**: JavaScript loads actual user data via REST API calls
3. **Authentication**: JWT token stored in localStorage, included in API requests
4. **Security**: API endpoints enforce authentication and organization-level isolation

**Benefits:**
- Fast initial page load (no server-side user lookup)
- Better caching of static HTML
- Cleaner separation between presentation and data
- Easier to scale (stateless server-side rendering)

**Flow:**
1. User navigates to `/dashboard`
2. Server returns static HTML with placeholder data
3. Alpine.js `init()` function runs on page load
4. JavaScript checks for JWT token in localStorage
5. If no token, redirects to `/auth/login`
6. If token exists, fetches user data from `/api/auth/me`
7. Fetches dashboard stats from `/dashboard/stats`
8. Fetches recent activity from `/dashboard/activity`
9. Updates UI with real data

### Wizard Architecture

The agent creation wizard follows a session-based approach:

**Session Management:**
- Wizard sessions stored in Redis with 1-hour expiration
- Each session tracks current step and step data
- Session ownership verified on all operations

**Wizard Steps:**
1. **PURPOSE** - Define agent name and description
2. **PROMPT** - Configure system prompt and behavior
3. **TOOLS** - Select API integrations (optional)
4. **PREVIEW** - Test agent with sample prompts
5. **DEPLOY** - Deploy agent to production

**Workflow:**
1. User clicks "Create Agent" from dashboard
2. Frontend calls `POST /api/wizard/session` to initialize
3. User completes each step, frontend calls `PUT /api/wizard/session/{id}/step`
4. User can validate config at any time with `POST /api/wizard/validate`
5. User tests agent with `POST /api/wizard/preview` (step 4)
6. User deploys with `POST /api/wizard/deploy`
7. Session automatically cleaned up after deployment

**Security:**
- All endpoints require JWT authentication
- Session ownership verified on every operation
- Organization-level isolation enforced at deployment
- Configuration validation prevents malformed agents

## Next Steps for Development

To continue implementation, follow the task breakdown in `.kiro/specs/ai-agent-platform/tasks.md`:

1. **Task 5.4**: Implement wizard validation logic
   - Validate agent name and description
   - Validate system prompt (length, content)
   - Validate tool selection
   - Check configuration completeness

2. **Task 5.5**: Write wizard service tests (optional)
   - Unit tests for wizard session management
   - Tests for configuration validation
   - Test agent deployment flow
   - Test preview functionality

3. **Task 6**: Implement LangChain4j AI services integration
   - Configure Mistral AI model provider
   - Create AI service interfaces with LangChain4j
   - Implement chat memory provider
   - Write AI service integration tests

4. **Tasks 7-19**: Continue with remaining features as documented

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
