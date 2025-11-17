# Project Structure

## Directory Organization

```
src/main/
├── java/                           # Java source code
│   └── com/platform/
│       ├── domain/                 # Domain entities and models
│       │   ├── Agent.java
│       │   ├── Tool.java
│       │   ├── Conversation.java
│       │   ├── Message.java
│       │   ├── Document.java
│       │   └── User.java
│       ├── service/                # Business logic services
│       │   ├── AgentWizardService.java
│       │   ├── AgentRuntimeService.java
│       │   ├── ToolRegistryService.java
│       │   ├── MonitoringService.java
│       │   ├── AuthenticationService.java
│       │   └── VectorStoreService.java
│       ├── repository/             # Data access layer (Panache)
│       │   ├── AgentRepository.java
│       │   ├── ToolRepository.java
│       │   └── UserRepository.java
│       ├── rest/                   # REST API endpoints
│       │   ├── AgentResource.java
│       │   ├── ToolResource.java
│       │   ├── AuthResource.java
│       │   └── MonitoringResource.java
│       ├── websocket/              # WebSocket endpoints
│       │   └── AgentChatWebSocket.java
│       ├── ai/                     # AI service interfaces
│       │   ├── AgentAIService.java
│       │   └── EmbeddingService.java
│       ├── security/               # Security and auth
│       │   ├── JwtTokenProvider.java
│       │   └── RBACPolicy.java
│       └── exception/              # Exception handlers
│           └── GlobalExceptionHandler.java
├── resources/
│   ├── templates/                  # Qute templates
│   │   ├── layout/
│   │   │   ├── base.html
│   │   │   └── dashboard.html
│   │   ├── auth/
│   │   │   ├── login.html
│   │   │   └── register.html
│   │   ├── wizard/
│   │   │   ├── step1-purpose.html
│   │   │   ├── step2-prompt.html
│   │   │   ├── step3-tools.html
│   │   │   └── step4-preview.html
│   │   ├── dashboard/
│   │   │   ├── home.html
│   │   │   └── monitoring.html
│   │   ├── tools/
│   │   │   ├── list.html
│   │   │   └── create.html
│   │   └── documents/
│   │       ├── list.html
│   │       └── upload.html
│   ├── META-INF/
│   │   └── resources/              # Static assets
│   │       ├── css/
│   │       ├── js/
│   │       └── images/
│   ├── db/migration/               # Database migrations (Flyway)
│   │   ├── V1__create_users_orgs.sql
│   │   ├── V2__create_agents_tools.sql
│   │   ├── V3__create_conversations.sql
│   │   └── V4__create_documents_vectors.sql
│   └── application.properties      # Configuration
└── test/                           # Test code
    └── java/
        └── com/platform/
            ├── service/            # Service tests
            ├── rest/               # API tests
            └── integration/        # Integration tests
```

## Architectural Patterns

### Layered Architecture
- **Presentation Layer**: Qute templates + REST endpoints
- **Application Layer**: Service classes with business logic
- **Domain Layer**: Entity models with Panache
- **Integration Layer**: External API clients and AI services
- **Data Layer**: PostgreSQL + Redis

### Key Conventions

1. **Entities**: Use Panache for simplified repository pattern
2. **Services**: Annotate with `@ApplicationScoped` for CDI
3. **REST Resources**: Use JAX-RS annotations, path prefix `/api`
4. **Templates**: Qute templates in `resources/templates/`, use Tailwind CSS
5. **Configuration**: Externalize in `application.properties`
6. **Error Handling**: Global exception mapper for consistent responses
7. **Security**: JWT-based auth, `@RolesAllowed` for authorization
8. **Async**: Use `CompletionStage` and `Multi` for reactive operations
9. **Testing**: Unit tests for services, integration tests with TestContainers

### Naming Conventions

- **Entities**: Singular nouns (Agent, Tool, User)
- **Services**: `<Entity>Service` pattern (AgentWizardService)
- **Repositories**: `<Entity>Repository` pattern (AgentRepository)
- **REST Resources**: `<Entity>Resource` pattern (AgentResource)
- **Templates**: Kebab-case (agent-wizard.html, tool-list.html)
- **Database Tables**: Plural snake_case (agents, tools, conversations)

### Multi-Tenancy

Organization-level isolation enforced at:
- Database queries (filter by organization_id)
- REST endpoints (validate user's organization)
- UI templates (show only org-scoped data)
