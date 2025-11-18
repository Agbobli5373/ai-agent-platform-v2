# Tool Registry and Execution Service - Testing Guide

## Overview
This document describes how to test the Tool Registry and Execution Service implementation using Playwright or manual testing.

## Prerequisites
- Application running on `http://localhost:8080`
- User authenticated with appropriate permissions
- Test API endpoint available (e.g., `https://api.example.com/test`)

## Test Scenarios

### 1. Tool Registration (API)

**Endpoint:** `POST /api/tools`

**Test Case 1.1: Register REST API Tool with API Key Authentication**
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

**Expected Result:**
- Status: 201 Created
- Response contains tool ID, name, and configuration
- Tool appears in organization's tool list

**Test Case 1.2: Register Tool with Basic Authentication**
```json
{
  "name": "Internal API",
  "description": "Company internal API",
  "type": "REST_API",
  "endpoint": "https://internal-api.company.com/data",
  "authConfig": {
    "type": "BASIC_AUTH",
    "username": "api_user",
    "password": "api_password"
  }
}
```

**Expected Result:**
- Status: 201 Created
- Credentials stored securely
- Tool can be validated

**Test Case 1.3: Register Tool with No Authentication**
```json
{
  "name": "Public API",
  "description": "Public data API",
  "type": "REST_API",
  "endpoint": "https://api.publicapis.org/entries",
  "authConfig": {
    "type": "NONE"
  }
}
```

**Expected Result:**
- Status: 201 Created
- Tool registered without authentication

### 2. Tool Validation

**Endpoint:** `POST /api/tools/{toolId}/validate`

**Test Case 2.1: Validate Working Tool**
- Register a tool pointing to a valid, accessible endpoint
- Call validation endpoint
- **Expected Result:**
  - Status: 200 OK
  - Response: `{ "valid": true, "message": "Connection successful", "responseTimeMs": <time>, "statusCode": 200 }`

**Test Case 2.2: Validate Tool with Invalid Endpoint**
- Register a tool with non-existent endpoint
- Call validation endpoint
- **Expected Result:**
  - Status: 400 Bad Request
  - Response: `{ "valid": false, "message": "Connection error: ...", "errorDetails": "..." }`

**Test Case 2.3: Validate Tool with Authentication Failure**
- Register a tool with incorrect credentials
- Call validation endpoint
- **Expected Result:**
  - Status: 400 Bad Request
  - Response indicates authentication failure (401/403 status)

### 3. Tool Execution

**Endpoint:** `POST /api/tools/{toolId}/execute`

**Test Case 3.1: Execute Tool Successfully**
```json
{
  "city": "London",
  "units": "metric"
}
```

**Expected Result:**
- Status: 200 OK
- Response contains execution result
- Execution time logged
- Result formatted as JSON

**Test Case 3.2: Execute Tool with Timeout**
- Configure tool with very slow endpoint
- Execute tool
- **Expected Result:**
  - Timeout after 10 seconds
  - Error message indicates timeout
  - Circuit breaker may activate after multiple failures

**Test Case 3.3: Execute Tool with Retry Logic**
- Use tool that fails intermittently
- Execute tool
- **Expected Result:**
  - Automatic retry up to 3 times
  - Exponential backoff between retries
  - Success if any retry succeeds

### 4. Tool Management UI

**Page:** `/tools`

**Test Case 4.1: View Tool List**
1. Navigate to `/tools`
2. **Expected Result:**
   - List of all tools for organization displayed
   - Each tool shows: name, description, type, endpoint
   - Action buttons: Test, Edit, Delete

**Test Case 4.2: Create Tool via UI**
1. Navigate to `/tools/create`
2. Fill in form:
   - Name: "Test API"
   - Description: "Test description"
   - Type: REST_API
   - Endpoint: "https://api.example.com/test"
   - Authentication: API Key
   - API Key: "test-key"
3. Click "Create Tool"
4. **Expected Result:**
   - Success message displayed
   - Redirected to tool list
   - New tool appears in list

**Test Case 4.3: Test Tool Connection from UI**
1. Navigate to `/tools`
2. Click "Test" button on a tool
3. **Expected Result:**
   - Loading indicator shown
   - Result displayed inline (green for success, red for failure)
   - Response time shown

**Test Case 4.4: Edit Tool**
1. Navigate to `/tools`
2. Click "Edit" on a tool
3. Modify tool details
4. Click "Update Tool"
5. **Expected Result:**
   - Success message displayed
   - Changes saved
   - Redirected to tool list

**Test Case 4.5: Delete Tool**
1. Navigate to `/tools`
2. Click "Delete" on a tool
3. Confirm deletion
4. **Expected Result:**
   - Tool removed from list
   - Page refreshed
   - Tool no longer accessible

### 5. Tool Selection in Agent Wizard

**Page:** `/wizard` (Step 3)

**Test Case 5.1: Select Tools for Agent**
1. Navigate to agent creation wizard
2. Proceed to Step 3 (Tools)
3. **Expected Result:**
   - All organization tools displayed
   - Checkboxes for selection
   - Tool details shown (name, description, type, endpoint)

**Test Case 5.2: Create Agent with No Tools**
1. In wizard Step 3, select no tools
2. Proceed to next step
3. **Expected Result:**
   - Agent created successfully
   - No tools associated with agent

**Test Case 5.3: Create Agent with Multiple Tools**
1. In wizard Step 3, select 2-3 tools
2. Complete wizard
3. **Expected Result:**
   - Agent created with selected tools
   - Tools available for agent execution

### 6. LangChain4j Tool Integration

**Test Case 6.1: Execute Tool via LangChain4j**
- Use LangChainToolProvider.executeTool()
- Pass tool ID and parameters as JSON
- **Expected Result:**
  - Tool executed successfully
  - Result formatted for LLM consumption
  - Execution logged

**Test Case 6.2: List Available Tools**
- Call LangChainToolProvider.listAvailableTools()
- **Expected Result:**
  - Returns formatted list of all tools
  - Includes tool IDs, names, and descriptions

**Test Case 6.3: Get Tool Info**
- Call LangChainToolProvider.getToolInfo()
- **Expected Result:**
  - Returns detailed tool information as JSON
  - Includes all tool metadata

### 7. Error Handling

**Test Case 7.1: Invalid Tool ID**
- Request tool with non-existent ID
- **Expected Result:**
  - Status: 404 Not Found
  - Error message: "Tool not found"

**Test Case 7.2: Unauthorized Access**
- Attempt to access tool from different organization
- **Expected Result:**
  - Status: 403 Forbidden or 404 Not Found
  - Access denied

**Test Case 7.3: Invalid Tool Configuration**
- Register tool with invalid endpoint URL
- **Expected Result:**
  - Status: 400 Bad Request
  - Validation error message

**Test Case 7.4: Circuit Breaker Activation**
- Execute failing tool multiple times (>10 requests)
- **Expected Result:**
  - Circuit breaker opens after 50% failure rate
  - Subsequent requests fail fast for 30 seconds
  - Circuit breaker closes after delay

### 8. Performance Tests

**Test Case 8.1: Tool Execution Timeout**
- Execute tool that takes >10 seconds
- **Expected Result:**
  - Request times out at 10 seconds
  - Timeout error returned

**Test Case 8.2: Concurrent Tool Execution**
- Execute multiple tools simultaneously
- **Expected Result:**
  - All tools execute in parallel
  - Results aggregated correctly
  - No race conditions

**Test Case 8.3: Tool Validation Performance**
- Validate tool connection
- **Expected Result:**
  - Validation completes within 5 seconds
  - Response time measured and returned

## Playwright Test Script Example

```javascript
// Example Playwright test for tool creation
test('Create and test tool via UI', async ({ page }) => {
  // Navigate to tools page
  await page.goto('http://localhost:8080/tools');
  
  // Click create tool button
  await page.click('text=Create Tool');
  
  // Fill in form
  await page.fill('#name', 'Test Weather API');
  await page.fill('#description', 'Test weather data');
  await page.selectOption('#type', 'REST_API');
  await page.fill('#endpoint', 'https://api.openweathermap.org/data/2.5/weather');
  
  // Configure authentication
  await page.selectOption('select[x-model="formData.authConfig.type"]', 'API_KEY');
  await page.fill('#apiKey', 'test-api-key');
  
  // Submit form
  await page.click('button[type="submit"]');
  
  // Wait for redirect
  await page.waitForURL('**/tools');
  
  // Verify tool appears in list
  await expect(page.locator('text=Test Weather API')).toBeVisible();
  
  // Test the tool
  await page.click('button:has-text("Test")');
  
  // Wait for test result
  await page.waitForSelector('[x-show="testResult"]');
  
  // Verify result displayed
  const result = await page.locator('[x-show="testResult"]').textContent();
  expect(result).toContain('Connection');
});
```

## Manual Testing Checklist

- [ ] Tool registration via API works
- [ ] Tool validation returns correct results
- [ ] Tool execution with different auth types works
- [ ] Retry logic activates on failures
- [ ] Circuit breaker prevents cascading failures
- [ ] Tool list UI displays correctly
- [ ] Tool creation form works
- [ ] Tool editing works
- [ ] Tool deletion works
- [ ] Tool test button provides feedback
- [ ] Wizard tool selection works
- [ ] LangChain4j integration executes tools
- [ ] Error messages are user-friendly
- [ ] Timeouts work as expected
- [ ] Authentication methods work correctly

## Notes

- All tests assume the application is running and accessible
- Authentication tokens/sessions must be valid
- Test data should be cleaned up after testing
- Performance tests should be run in isolation
- Circuit breaker tests may affect other tests if not isolated
