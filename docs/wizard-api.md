# Wizard REST API Documentation

## Overview

The Wizard REST API provides endpoints for creating AI agents through a guided wizard interface. All endpoints require authentication via JWT token.

## Base Path

```
/api/wizard
```

## Endpoints

### 1. Initialize Wizard Session

Creates a new wizard session for the authenticated user.

**Endpoint:** `POST /api/wizard/session`

**Authentication:** Required

**Request Body:** None

**Response:**
```json
{
  "sessionId": "uuid",
  "currentStep": "PURPOSE"
}
```

**Status Codes:**
- `201 Created`: Session created successfully
- `401 Unauthorized`: Authentication required

---

### 2. Get Wizard Session

Retrieves an existing wizard session.

**Endpoint:** `GET /api/wizard/session/{sessionId}`

**Authentication:** Required

**Path Parameters:**
- `sessionId` (UUID): The session identifier

**Response:**
```json
{
  "sessionId": "uuid",
  "userId": "uuid",
  "currentStep": "PROMPT",
  "stepData": {
    "name": "My Agent",
    "description": "Agent description"
  }
}
```

**Status Codes:**
- `200 OK`: Session retrieved successfully
- `404 Not Found`: Session not found or expired
- `403 Forbidden`: Session belongs to another user
- `401 Unauthorized`: Authentication required

---

### 3. Save Wizard Step

Saves data for a specific wizard step.

**Endpoint:** `PUT /api/wizard/session/{sessionId}/step`

**Authentication:** Required

**Path Parameters:**
- `sessionId` (UUID): The session identifier

**Request Body:**
```json
{
  "step": "PROMPT",
  "data": {
    "systemPrompt": "You are a helpful assistant...",
    "modelName": "mistral-large-latest"
  }
}
```

**Wizard Steps:**
- `PURPOSE`: Agent name and description
- `PROMPT`: System prompt configuration
- `TOOLS`: Tool selection
- `PREVIEW`: Preview and testing

**Response:**
```json
{
  "message": "Step saved successfully"
}
```

**Status Codes:**
- `200 OK`: Step saved successfully
- `404 Not Found`: Session not found or expired
- `403 Forbidden`: Session belongs to another user
- `400 Bad Request`: Invalid step or data
- `401 Unauthorized`: Authentication required

---

### 4. Validate Configuration

Validates an agent configuration without deploying it.

**Endpoint:** `POST /api/wizard/validate`

**Authentication:** Required

**Request Body:**
```json
{
  "name": "My Agent",
  "description": "Optional description",
  "systemPrompt": "You are a helpful assistant that...",
  "modelName": "mistral-large-latest",
  "toolIds": ["uuid1", "uuid2"]
}
```

**Response (Valid):**
```json
{
  "valid": true,
  "errors": []
}
```

**Response (Invalid):**
```json
{
  "valid": false,
  "errors": [
    "Agent name is required",
    "System prompt must be at least 10 characters"
  ]
}
```

**Validation Rules:**
- Name: Required, 3-255 characters
- Description: Optional, max 1000 characters
- System Prompt: Required, 10-10000 characters
- Model Name: Required

**Status Codes:**
- `200 OK`: Configuration is valid
- `400 Bad Request`: Configuration is invalid
- `401 Unauthorized`: Authentication required

---

### 5. Preview Agent

Tests an agent configuration with a sample prompt before deployment.

**Endpoint:** `POST /api/wizard/preview`

**Authentication:** Required

**Request Body:**
```json
{
  "config": {
    "name": "My Agent",
    "description": "Optional description",
    "systemPrompt": "You are a helpful assistant...",
    "modelName": "mistral-large-latest",
    "toolIds": []
  },
  "testPrompt": "Hello, how can you help me?"
}
```

**Response:**
```json
{
  "response": "Preview response from agent 'My Agent': This is a simulated response...",
  "responseTimeMs": 150
}
```

**Status Codes:**
- `200 OK`: Preview generated successfully
- `400 Bad Request`: Invalid configuration or missing test prompt
- `401 Unauthorized`: Authentication required

---

### 6. Deploy Agent

Deploys a configured agent and makes it active.

**Endpoint:** `POST /api/wizard/deploy`

**Authentication:** Required

**Request Body:**
```json
{
  "config": {
    "name": "My Agent",
    "description": "Optional description",
    "systemPrompt": "You are a helpful assistant...",
    "modelName": "mistral-large-latest",
    "toolIds": ["uuid1", "uuid2"]
  },
  "sessionId": "uuid"
}
```

**Note:** The `sessionId` field is optional. If provided, the wizard session will be automatically deleted after successful deployment.

**Response:**
```json
{
  "agentId": "uuid",
  "name": "My Agent",
  "status": "ACTIVE"
}
```

**Status Codes:**
- `201 Created`: Agent deployed successfully
- `400 Bad Request`: Invalid configuration
- `403 Forbidden`: Session belongs to another user (if sessionId provided)
- `401 Unauthorized`: Authentication required

---

### 7. Delete Wizard Session

Deletes a wizard session and its associated data.

**Endpoint:** `DELETE /api/wizard/session/{sessionId}`

**Authentication:** Required

**Path Parameters:**
- `sessionId` (UUID): The session identifier

**Response:**
```json
{
  "message": "Session deleted successfully"
}
```

**Status Codes:**
- `200 OK`: Session deleted successfully
- `403 Forbidden`: Session belongs to another user
- `401 Unauthorized`: Authentication required

---

## Example Workflow

### Complete Agent Creation Flow

1. **Initialize Session**
```bash
curl -X POST http://localhost:8080/api/wizard/session \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

2. **Save Purpose Step**
```bash
curl -X PUT http://localhost:8080/api/wizard/session/{sessionId}/step \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "step": "PURPOSE",
    "data": {
      "name": "Customer Support Agent",
      "description": "Handles customer inquiries"
    }
  }'
```

3. **Save Prompt Step**
```bash
curl -X PUT http://localhost:8080/api/wizard/session/{sessionId}/step \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "step": "PROMPT",
    "data": {
      "systemPrompt": "You are a helpful customer support agent...",
      "modelName": "mistral-large-latest"
    }
  }'
```

4. **Validate Configuration**
```bash
curl -X POST http://localhost:8080/api/wizard/validate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Support Agent",
    "description": "Handles customer inquiries",
    "systemPrompt": "You are a helpful customer support agent...",
    "modelName": "mistral-large-latest"
  }'
```

5. **Preview Agent**
```bash
curl -X POST http://localhost:8080/api/wizard/preview \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "config": {
      "name": "Customer Support Agent",
      "systemPrompt": "You are a helpful customer support agent...",
      "modelName": "mistral-large-latest"
    },
    "testPrompt": "How do I reset my password?"
  }'
```

6. **Deploy Agent**
```bash
curl -X POST http://localhost:8080/api/wizard/deploy \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "config": {
      "name": "Customer Support Agent",
      "description": "Handles customer inquiries",
      "systemPrompt": "You are a helpful customer support agent...",
      "modelName": "mistral-large-latest"
    },
    "sessionId": "{sessionId}"
  }'
```

## Error Responses

All endpoints return consistent error responses:

```json
{
  "error": "Error message describing what went wrong"
}
```

Common error scenarios:
- Missing or invalid JWT token: `401 Unauthorized`
- Session not found or expired: `404 Not Found`
- Access to another user's session: `403 Forbidden`
- Invalid request data: `400 Bad Request`
- Validation failures: `400 Bad Request` with validation errors

## Session Management

- Sessions are stored in Redis with a 2-hour expiration
- Sessions are automatically deleted after successful agent deployment (if sessionId is provided)
- Sessions can be manually deleted using the DELETE endpoint
- Each session is tied to a specific user and cannot be accessed by other users
