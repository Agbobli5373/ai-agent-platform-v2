# Testing the Wizard UI

## Prerequisites

1. **Start Required Services**

Make sure PostgreSQL and Redis are running:

```bash
# PostgreSQL with pgvector
docker run -d --name postgres -e POSTGRES_PASSWORD=password -p 5432:5432 ankane/pgvector

# Redis
docker run -d --name redis -p 6379:6379 redis:latest
```

2. **Start the Application**

```bash
./mvnw quarkus:dev
```

Wait for the application to start. You should see:
```
Listening on: http://localhost:8080
```

## Step-by-Step Testing Guide

### 1. Register/Login

First, you need to be authenticated:

**Option A: Register a new account**
- Navigate to: `http://localhost:8080/auth/register`
- Fill in your details and register

**Option B: Login with existing account**
- Navigate to: `http://localhost:8080/auth/login`
- Enter your credentials

### 2. Access the Wizard

Once logged in, navigate to:
```
http://localhost:8080/dashboard/wizard
```

### 3. Test Each Wizard Step

#### Step 1: Purpose
- **What to test:**
  - Enter an agent name (e.g., "Customer Support Bot")
  - Add an optional description
  - Try entering less than 3 characters - should show validation error
  - Try entering more than 255 characters - should show validation error
  - Click "Next" when valid

- **Expected behavior:**
  - Validation errors appear in red below the input
  - "Next" button is disabled when validation fails
  - Progress indicator shows step 1 as active (blue)

#### Step 2: Prompt
- **What to test:**
  - Enter a system prompt (e.g., "You are a helpful customer support agent...")
  - Try different AI models from the dropdown
  - Try entering less than 10 characters - should show validation error
  - Read the prompt tips for guidance
  - Click "Next" when valid

- **Expected behavior:**
  - Validation errors appear for prompts that are too short
  - Model selection updates the config
  - Progress indicator shows step 2 as active
  - Step 1 shows as completed (green checkmark)

#### Step 3: Tools (Optional)
- **What to test:**
  - If you have tools created, they should appear as checkboxes
  - Select/deselect tools
  - If no tools exist, you'll see a "Create Your First Tool" button
  - You can skip this step by clicking "Next"

- **Expected behavior:**
  - Tools load from the API
  - Selected tools are tracked in the config
  - Can proceed without selecting any tools
  - Progress indicator shows step 3 as active

#### Step 4: Preview & Deploy
- **What to test:**
  - Review the configuration summary
  - Enter a test prompt (e.g., "Hello, how can you help me?")
  - Click "Test Agent" to preview
  - Review the mock response
  - Click "Deploy Agent" to create the agent

- **Expected behavior:**
  - Configuration summary displays correctly
  - Test button is disabled when prompt is empty
  - Preview shows a simulated response with response time
  - Deploy button creates the agent and redirects to agents list
  - Progress indicator shows step 4 as active

### 4. Test Navigation

- **Back Button:** Click "Back" to return to previous steps
- **Save Draft:** Click "Save Draft" at any step to save progress
- **Session Persistence:** The wizard session is stored in Redis for 2 hours

### 5. Test Error Scenarios

#### Invalid Configuration
1. Try to proceed without filling required fields
2. Enter invalid data (too short/long)
3. Verify validation messages appear

#### Network Errors
1. Stop the backend server
2. Try to initialize a session or deploy
3. Should see error messages

#### Session Expiration
1. Wait 2 hours (or manually delete the Redis key)
2. Try to continue the wizard
3. Should handle expired session gracefully

## Monitoring the Backend

### Check API Calls in Browser DevTools

1. Open browser DevTools (F12)
2. Go to the "Network" tab
3. Watch for these API calls as you use the wizard:

```
POST /api/wizard/session          - Initialize session
PUT  /api/wizard/session/{id}/step - Save step data
GET  /api/tools                    - Load available tools
POST /api/wizard/validate          - Validate configuration
POST /api/wizard/preview           - Preview agent
POST /api/wizard/deploy            - Deploy agent
```

### Check Server Logs

Watch the Quarkus dev console for:
- Session creation logs
- Validation errors
- Deployment success/failure
- Any exceptions

### Check Redis Session Data

```bash
# Connect to Redis
docker exec -it redis redis-cli

# List wizard sessions
KEYS wizard:session:*

# View a session
GET wizard:session:{session-id}
```

### Check Database

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres

# Check created agents
SELECT id, name, status, created_at FROM agents;

# Check agent tools
SELECT * FROM agent_tools;
```

## Common Issues and Solutions

### Issue: "Failed to initialize wizard session"
**Solution:** 
- Check if Redis is running: `docker ps | grep redis`
- Check application.properties for Redis configuration
- Verify JWT token is valid

### Issue: "Session not found or expired"
**Solution:**
- Session expired after 2 hours
- Start a new wizard session
- Check Redis is running

### Issue: Tools not loading
**Solution:**
- Check if `/api/tools` endpoint is accessible
- Verify you have tools created in the database
- Check browser console for errors

### Issue: Deploy fails
**Solution:**
- Check validation errors in the response
- Verify all required fields are filled
- Check server logs for detailed error messages
- Ensure PostgreSQL is running

### Issue: Preview not working
**Solution:**
- Currently returns a mock response
- Check if the configuration is valid
- Verify the test prompt is not empty

## Next Steps After Testing

Once you've verified the wizard works:

1. **Create Real Tools:** Navigate to `/tools/create` to add actual tools
2. **Test with Real AI:** The preview currently uses mock responses. Integrate with Mistral AI for real responses
3. **View Created Agents:** Navigate to `/agents` to see your deployed agents
4. **Test Agent Chat:** Use the created agent in a conversation

## API Testing (Alternative)

If you prefer to test the API directly without the UI, see `docs/wizard-api.md` for curl examples.
