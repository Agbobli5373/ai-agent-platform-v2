# Wizard UI Testing Guide

## Overview

The wizard UI provides a step-by-step interface for creating AI agents without technical knowledge. This guide explains how to test the wizard feature.

## Prerequisites

1. Application running: `./mvnw quarkus:dev`
2. PostgreSQL database running
3. Redis running
4. User account created and logged in

## Accessing the Wizard

1. Start the application in dev mode:
```bash
./mvnw quarkus:dev
```

2. Open your browser and navigate to:
```
http://localhost:8080/auth/login
```

3. Log in with your credentials

4. Navigate to the wizard:
```
http://localhost:8080/wizard
```

Or click "Create Agent" from the dashboard.

## Testing the Wizard Flow

### Step 1: Purpose

**What to test:**
- Enter an agent name (3-255 characters)
- Optionally add a description (max 1000 characters)
- Validation should prevent proceeding with invalid input

**Test cases:**
1. Try entering a name with less than 3 characters - should show error
2. Try entering a name with more than 255 characters - should show error
3. Enter a valid name (e.g., "Customer Support Agent")
4. Add an optional description
5. Click "Next" - should proceed to Step 2

### Step 2: Prompt

**What to test:**
- Enter a system prompt (10-10000 characters)
- Select an AI model
- View prompt tips

**Test cases:**
1. Try entering a prompt with less than 10 characters - should show error
2. Enter a valid system prompt:
```
You are a helpful customer support agent for an e-commerce platform. 
Your role is to assist customers with order inquiries, product questions, 
and general support. Be friendly, professional, and concise in your responses.
```
3. Select different AI models from the dropdown
4. Click "Next" - should proceed to Step 3

### Step 3: Tools (Optional)

**What to test:**
- View available tools
- Select/deselect tools
- Handle empty tool list

**Test cases:**
1. If no tools exist, should show "No tools available" message
2. If tools exist, should display them as checkboxes
3. Select one or more tools
4. Click "Next" - should proceed to Step 4

### Step 4: Preview & Deploy

**What to test:**
- View configuration summary
- Test the agent with a sample prompt
- Deploy the agent

**Test cases:**
1. Verify configuration summary shows correct values
2. Enter a test prompt (e.g., "How do I track my order?")
3. Click "Test Agent" - should show a preview response
4. Verify response time is displayed
5. Click "Deploy Agent" - should create the agent and redirect to agents list

## UI Features to Test

### Progress Indicator
- Current step should be highlighted in blue
- Completed steps should show green checkmark
- Future steps should be gray
- Step names should be visible below circles

### Navigation
- "Back" button should work on steps 2-4
- "Next" button should be disabled if validation fails
- "Save Draft" should save current progress
- "Deploy Agent" should only appear on final step

### Responsive Design
- Test on desktop (1920x1080)
- Test on tablet (768x1024)
- Test on mobile (375x667)
- All elements should be accessible and usable

### Validation
- Real-time validation on name field
- Real-time validation on system prompt field
- Error messages should be clear and helpful
- Cannot proceed to next step with invalid data

## Expected Behavior

### Session Management
- Wizard creates a session on load
- Session data is saved after each step
- Session expires after 2 hours
- Session is deleted after successful deployment

### API Integration
- All API calls should complete within 2 seconds
- Loading states should be shown during API calls
- Errors should be displayed with clear messages
- Success messages should confirm actions

### Agent Deployment
- Agent should be created in database
- Agent status should be "ACTIVE"
- User should be redirected to agents list
- Success message should be displayed

## Common Issues and Solutions

### Issue: Wizard doesn't load
**Solution:** Check that you're logged in and have a valid JWT token

### Issue: "Session not found" error
**Solution:** Refresh the page to create a new session

### Issue: Tools don't load
**Solution:** Ensure the tools API endpoint is working: `GET /api/tools`

### Issue: Preview doesn't work
**Solution:** Check that the preview endpoint is accessible: `POST /api/wizard/preview`

### Issue: Deployment fails
**Solution:** 
1. Check validation errors in the response
2. Verify database connection
3. Check server logs for detailed error messages

## Browser Console Testing

Open browser developer tools (F12) and check:

1. **Network tab:** Verify API calls are successful
   - POST /api/wizard/session (201 Created)
   - PUT /api/wizard/session/{id}/step (200 OK)
   - POST /api/wizard/preview (200 OK)
   - POST /api/wizard/deploy (201 Created)

2. **Console tab:** Check for JavaScript errors
   - Should see no errors during normal operation
   - Alpine.js should initialize properly

3. **Application tab:** Verify localStorage
   - accessToken should be present
   - refreshToken should be present

## Manual Testing Checklist

- [ ] Can access wizard page when logged in
- [ ] Cannot access wizard page when logged out (redirects to login)
- [ ] Step 1: Can enter agent name and description
- [ ] Step 1: Validation works correctly
- [ ] Step 2: Can enter system prompt
- [ ] Step 2: Can select AI model
- [ ] Step 2: Validation works correctly
- [ ] Step 3: Can view and select tools
- [ ] Step 3: Works correctly with no tools
- [ ] Step 4: Configuration summary is accurate
- [ ] Step 4: Can test agent with preview
- [ ] Step 4: Preview shows response and timing
- [ ] Can navigate back and forth between steps
- [ ] Can save draft at any step
- [ ] Can deploy agent from final step
- [ ] Redirects to agents list after deployment
- [ ] Progress indicator updates correctly
- [ ] All buttons work as expected
- [ ] Responsive design works on mobile
- [ ] Loading states display correctly
- [ ] Error messages are clear and helpful

## Next Steps

After testing the wizard UI:
1. Test the agents list page to see deployed agents
2. Test agent interaction via chat interface
3. Test tool management if tools were selected
4. Test monitoring dashboard to see agent metrics
