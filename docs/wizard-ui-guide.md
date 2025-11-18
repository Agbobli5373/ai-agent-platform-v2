# Wizard UI Testing Guide

## Overview

The agent creation wizard provides a step-by-step interface for creating AI agents without technical expertise. The wizard consists of 4 steps with validation and preview capabilities.

## Accessing the Wizard

1. **Start the application:**
```bash
./mvnw quarkus:dev
```

2. **Login to the platform:**
   - Navigate to `http://localhost:8080/auth/login`
   - Login with your credentials
   - You'll be redirected to the dashboard

3. **Access the wizard:**
   - Click "Create Agent" button on the dashboard
   - Or navigate directly to `http://localhost:8080/wizard`

## Wizard Steps

### Step 1: Purpose
Define your agent's basic information:
- **Agent Name** (required): 3-255 characters
- **Description** (optional): Up to 1000 characters

**Validation:**
- Name is required and must be at least 3 characters
- Real-time validation as you type
- Cannot proceed without a valid name

### Step 2: Prompt
Configure how your agent behaves:
- **System Prompt** (required): 10-10,000 characters
- **AI Model** (required): Select from Mistral models

**Features:**
- Large textarea for detailed prompts
- Model selection dropdown
- Helpful tips for writing effective prompts
- Real-time character count validation

### Step 3: Tools (Optional)
Select tools your agent can use:
- View all available tools in your organization
- Select multiple tools via checkboxes
- See tool descriptions and types
- Can skip this step if no tools needed

**Note:** If no tools exist, you'll see a prompt to create one first.

### Step 4: Preview & Deploy
Test and deploy your agent:
- **Configuration Summary**: Review all settings
- **Test Chat**: Send a test message to preview responses
- **Deploy**: Launch your agent

**Features:**
- Configuration summary card
- Interactive test interface
- Real-time preview responses
- Response time metrics

## Features

### Progress Indicator
- Visual step tracker at the top
- Shows completed steps with checkmarks
- Highlights current step
- Shows upcoming steps

### Navigation
- **Back button**: Return to previous step
- **Next button**: Proceed to next step (disabled if validation fails)
- **Save Draft**: Save progress without deploying
- **Deploy Agent**: Final deployment (only on last step)

### Validation
- Real-time field validation
- Clear error messages
- Cannot proceed with invalid data
- Visual feedback for errors

### Session Management
- Wizard session stored in Redis
- 2-hour session expiration
- Auto-save on step navigation
- Session cleanup after deployment

## Testing Checklist

### Basic Flow
- [ ] Access wizard from dashboard
- [ ] Complete Step 1 with valid name
- [ ] Complete Step 2 with valid prompt
- [ ] Skip or select tools in Step 3
- [ ] Preview agent in Step 4
- [ ] Deploy agent successfully
- [ ] Redirect to agents list

### Validation Testing
- [ ] Try to proceed without agent name
- [ ] Try name with less than 3 characters
- [ ] Try to proceed without system prompt
- [ ] Try prompt with less than 10 characters
- [ ] Verify error messages display correctly

### Navigation Testing
- [ ] Use Back button to return to previous steps
- [ ] Use Next button to advance
- [ ] Verify completed steps show checkmarks
- [ ] Verify current step is highlighted

### Preview Testing
- [ ] Enter test prompt
- [ ] Click "Test Agent" button
- [ ] Verify response displays
- [ ] Verify response time shows

### Save Draft
- [ ] Click "Save Draft" at any step
- [ ] Verify success message
- [ ] Refresh page and verify data persists (future enhancement)

### Mobile Testing
- [ ] Access wizard on mobile device
- [ ] Verify responsive layout
- [ ] Test touch interactions
- [ ] Verify all steps work on small screens

## API Endpoints Used

The wizard UI interacts with these REST endpoints:

1. `POST /api/wizard/session` - Initialize wizard session
2. `PUT /api/wizard/session/{id}/step` - Save step data
3. `POST /api/wizard/validate` - Validate configuration
4. `POST /api/wizard/preview` - Preview agent response
5. `POST /api/wizard/deploy` - Deploy agent
6. `GET /api/tools` - Load available tools

## Troubleshooting

### Wizard doesn't load
- Check if you're logged in (JWT token in localStorage)
- Check browser console for errors
- Verify backend is running

### Session initialization fails
- Check Redis is running
- Verify JWT token is valid
- Check backend logs

### Tools don't load
- Verify tools exist in your organization
- Check API endpoint `/api/tools` is accessible
- Check browser network tab for errors

### Preview doesn't work
- Verify all required fields are filled
- Check configuration is valid
- Verify Mistral AI is configured

### Deployment fails
- Check all validation passes
- Verify database is accessible
- Check backend logs for errors

## Browser Compatibility

Tested and supported on:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Known Limitations

1. Draft saving doesn't persist across sessions (requires session restoration)
2. Preview uses mock responses (until AI service is integrated)
3. Tool list doesn't refresh automatically (requires page reload)
4. No undo/redo functionality
5. No wizard progress persistence across browser sessions

## Future Enhancements

- [ ] Add wizard progress persistence
- [ ] Implement real AI preview responses
- [ ] Add agent templates
- [ ] Add more validation rules
- [ ] Add inline help tooltips
- [ ] Add keyboard shortcuts
- [ ] Add wizard tour for first-time users
- [ ] Add ability to edit deployed agents
