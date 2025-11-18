# Context7 Integration Guide - Tool Registry Implementation

## Overview
This guide explains how the Tool Registry and Execution Service integrates with LangChain4j using Context7 documentation patterns.

## LangChain4j Tool Integration Pattern

### 1. Tool Definition with @Tool Annotation

Based on Context7 documentation from `/quarkiverse/quarkus-langchain4j`, tools are defined using the `@Tool` annotation:

```java
@ApplicationScoped
public class LangChainToolProvider {
    
    @Tool("Execute a registered tool by its ID with the given parameters")
    public String executeTool(
            @P("The UUID of the tool to execute") String toolId,
            @P("JSON string containing the parameters for the tool") String parameters) {
        // Implementation
    }
}
```

**Key Points:**
- `@Tool` annotation makes methods available to LLM
- Description helps the model understand usage
- `@P` annotation documents parameters for the LLM
- Methods must be in `@ApplicationScoped` beans

### 2. Tool Registration Patterns

#### Global Tool Registration
```java
@RegisterAiService(tools = LangChainToolProvider.class)
public interface AgentAIService {
    String chat(String systemPrompt, String userMessage);
}
```

#### Method-Specific Tools
```java
@RegisterAiService
public interface AgentAIService {
    @ToolBox(LangChainToolProvider.class)
    String chat(String systemPrompt, String userMessage);
}
```

### 3. Our Implementation

#### LangChainToolProvider.java
```java
@ApplicationScoped
public class LangChainToolProvider {

    @Inject
    ToolExecutor toolExecutor;

    @Inject
    ToolRepository toolRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Execute a registered tool by ID.
     * This method is exposed to LangChain4j as a tool that agents can call.
     */
    @Tool("Execute a registered tool by its ID with the given parameters")
    public String executeTool(
            @P("The UUID of the tool to execute") String toolId,
            @P("JSON string containing the parameters for the tool") String parameters) {
        
        try {
            UUID uuid = UUID.fromString(toolId);
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));
            
            Map<String, Object> params = parseParameters(parameters);
            ToolExecutionResult result = toolExecutor.execute(tool, params);
            
            return formatResultForLLM(result);
        } catch (Exception e) {
            return formatError(e);
        }
    }

    /**
     * List all available tools for the current agent.
     */
    @Tool("List all available tools that can be executed")
    public String listAvailableTools(@P("The organization ID") String organizationId) {
        try {
            UUID orgId = UUID.fromString(organizationId);
            List<Tool> tools = toolRepository.findByOrganization(orgId);
            
            StringBuilder result = new StringBuilder("Available tools:\n");
            for (Tool tool : tools) {
                result.append(String.format("- %s (ID: %s): %s\n",
                        tool.name, tool.id, tool.description));
            }
            return result.toString();
        } catch (Exception e) {
            return "Error listing tools: " + e.getMessage();
        }
    }

    /**
     * Get detailed information about a specific tool.
     */
    @Tool("Get detailed information about a specific tool")
    public String getToolInfo(@P("The UUID of the tool") String toolId) {
        try {
            UUID uuid = UUID.fromString(toolId);
            Tool tool = toolRepository.findByIdOptional(uuid)
                    .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));
            
            Map<String, Object> info = Map.of(
                    "id", tool.id.toString(),
                    "name", tool.name,
                    "description", tool.description != null ? tool.description : "No description",
                    "type", tool.type.toString(),
                    "endpoint", tool.endpoint
            );
            
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            return formatError(e);
        }
    }
}
```

## Tool Execution Flow

### 1. Agent Requests Tool Execution
```
User → Agent → LLM → LangChainToolProvider.executeTool()
```

### 2. Tool Execution with Fault Tolerance
```java
@ApplicationScoped
public class ToolExecutor {
    
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
    @CircuitBreakerName("tool-execution")
    public ToolExecutionResult execute(Tool tool, Map<String, Object> params) {
        // Execute HTTP request with authentication
        // Handle response and errors
        // Return formatted result
    }
}
```

### 3. Result Formatting for LLM
```java
private String formatResultForLLM(ToolExecutionResult result) {
    if (result.success) {
        return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "result", result.result,
                "executionTime", result.executionTimeMs + "ms"
        ));
    } else {
        return objectMapper.writeValueAsString(Map.of(
                "success", false,
                "error", result.errorMessage,
                "executionTime", result.executionTimeMs + "ms"
        ));
    }
}
```

## Authentication Patterns

### API Key Authentication
```java
case API_KEY:
    if (authConfig.apiKey != null) {
        requestBuilder.header("Authorization", "Bearer " + authConfig.apiKey);
        requestBuilder.header("X-API-Key", authConfig.apiKey);
    }
    break;
```

### Basic Authentication
```java
case BASIC_AUTH:
    if (authConfig.username != null && authConfig.password != null) {
        String credentials = authConfig.username + ":" + authConfig.password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        requestBuilder.header("Authorization", "Basic " + encodedCredentials);
    }
    break;
```

### OAuth2 Authentication
```java
case OAUTH2:
    if (authConfig.apiKey != null) {
        requestBuilder.header("Authorization", "Bearer " + authConfig.apiKey);
    }
    break;
```

## Fault Tolerance Patterns

### 1. Timeout
```java
@Timeout(value = 10, unit = ChronoUnit.SECONDS)
```
- Prevents long-running operations
- Returns error after 10 seconds
- Requirement 3.4 compliance

### 2. Retry with Exponential Backoff
```java
@Retry(maxRetries = 3, delay = 1000, jitter = 500)
```
- 3 retry attempts
- 1 second base delay
- 500ms jitter for randomization
- Exponential backoff between retries

### 3. Circuit Breaker
```java
@CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 30000)
@CircuitBreakerName("tool-execution")
```
- Opens after 10 requests with 50% failure rate
- Stays open for 30 seconds
- Prevents cascading failures
- Requirement 7.2 compliance

## Tool Orchestration

### Executing Multiple Tools
```java
@ApplicationScoped
public class ToolExecutionOrchestrator {
    
    @Inject
    ToolExecutor toolExecutor;
    
    public CompletionStage<ToolChainResult> executeToolChain(
            Agent agent,
            List<String> toolIds,
            Map<String, Object> context) {
        
        List<CompletableFuture<ToolExecutionResult>> futures = new ArrayList<>();
        
        for (String toolId : toolIds) {
            Tool tool = findToolById(agent, toolId);
            CompletableFuture<ToolExecutionResult> future = 
                    executeToolWithRetry(tool, context).toCompletableFuture();
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> aggregateResults(futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())));
    }
}
```

## Integration with Agent Wizard

### Step 3: Tool Selection
```html
<!-- wizard/step3-tools.html -->
<div x-show="currentStep === 3">
    <h2>Select Tools</h2>
    <div class="space-y-3">
        {#for tool in tools}
        <label class="flex items-start p-4 border rounded-lg hover:bg-gray-50">
            <input 
                type="checkbox" 
                :value="'{tool.id}'"
                x-model="formData.toolIds"
                class="mt-1 h-4 w-4 text-blue-600"
            />
            <div class="ml-3 flex-1">
                <span class="text-sm font-medium">{tool.name}</span>
                <p class="text-sm text-gray-600">{tool.description}</p>
                <p class="text-xs text-gray-500">{tool.endpoint}</p>
            </div>
        </label>
        {/for}
    </div>
</div>
```

## REST API Integration

### Tool Management Endpoints
```java
@Path("/api/tools")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ToolResource {
    
    @POST
    @RequiresPermission(Permission.TOOL_CREATE)
    public Response registerTool(ToolRegistrationRequest request) {
        User currentUser = getCurrentUser();
        Tool tool = toolRegistryService.registerTool(request, currentUser);
        return Response.status(Response.Status.CREATED).entity(tool).build();
    }
    
    @POST
    @Path("/{toolId}/validate")
    @RequiresPermission(Permission.TOOL_READ)
    public Response validateConnection(@PathParam("toolId") UUID toolId) {
        ToolValidationResult result = toolRegistryService.validateConnection(toolId);
        return result.valid ? Response.ok(result).build() 
                           : Response.status(Response.Status.BAD_REQUEST).entity(result).build();
    }
    
    @POST
    @Path("/{toolId}/execute")
    @RequiresPermission(Permission.TOOL_READ)
    public Response executeTool(@PathParam("toolId") UUID toolId, Map<String, Object> params) {
        ToolExecutionResult result = toolRegistryService.executeTool(toolId, params);
        return result.success ? Response.ok(result).build()
                             : Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(result).build();
    }
}
```

## Best Practices

### 1. Tool Naming
- Use descriptive names that explain what the tool does
- Follow snake_case for parameters (LLM compatibility)
- Provide clear descriptions in `@Tool` annotation

### 2. Error Handling
- Always return formatted JSON responses
- Include error details for debugging
- Log all tool executions for audit trail

### 3. Security
- Validate all inputs
- Enforce organization-level isolation
- Use proper authentication for external APIs
- Store credentials securely

### 4. Performance
- Use async execution for multiple tools
- Implement proper timeout values
- Use circuit breakers to prevent cascading failures
- Cache tool configurations when appropriate

### 5. Testing
- Test with real API endpoints
- Verify timeout behavior
- Test retry logic with intermittent failures
- Validate circuit breaker activation

## Example Usage

### Creating a Weather Tool
```java
// 1. Register tool via API
POST /api/tools
{
  "name": "Weather API",
  "description": "Get weather information for any city",
  "type": "REST_API",
  "endpoint": "https://api.openweathermap.org/data/2.5/weather",
  "authConfig": {
    "type": "API_KEY",
    "apiKey": "your-api-key"
  }
}

// 2. Agent uses tool via LangChain4j
User: "What's the weather in Paris?"
Agent: [Calls LangChainToolProvider.executeTool(weatherToolId, {city: "Paris"})]
Tool: Returns weather data
Agent: "The weather in Paris is rainy with a temperature of 15°C"
```

## References

- **Context7 Library:** `/quarkiverse/quarkus-langchain4j`
- **Quarkus Guides:** `/websites/quarkus_io_guides`
- **LangChain4j Docs:** https://docs.langchain4j.dev/
- **Quarkus Fault Tolerance:** https://quarkus.io/guides/smallrye-fault-tolerance

## Related Files

- `src/main/java/com/platform/service/LangChainToolProvider.java`
- `src/main/java/com/platform/service/ToolExecutor.java`
- `src/main/java/com/platform/service/ToolRegistryService.java`
- `src/main/java/com/platform/service/ToolExecutionOrchestrator.java`
- `src/main/java/com/platform/rest/ToolResource.java`
- `src/main/resources/templates/wizard/step3-tools.html`
