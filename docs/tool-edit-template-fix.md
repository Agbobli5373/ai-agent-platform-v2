# Tool Edit Template Fix

## Issue Summary

The tool edit page was throwing template errors when trying to edit, test, or delete tools:

```
Key "tool" not found in the template data map with keys [toolId, userInitials, userEmail, userName, userRole, currentOrgName] 
in expression {tool.id}
```

## Root Cause

The `tools/edit.html` template was trying to use `{tool.id}` as a Qute template variable, but the `ToolPageResource` was only passing `toolId` as a string. The JavaScript code was also trying to parse the URL to extract the tool ID instead of using the passed parameter.

## Solution

### 1. Pass toolId to Alpine.js Component

**Changed:**
```html
<div x-data="toolEditForm()" x-init="init()">
```

**To:**
```html
<div x-data="toolEditForm('{toolId}')" x-init="init()">
```

This passes the Qute template variable `{toolId}` directly to the Alpine.js component.

### 2. Update JavaScript Function to Accept toolId Parameter

**Changed:**
```javascript
function toolEditForm() {
    return {
        formData: { ... },
        async init() {
            const toolId = window.location.pathname.split('/')[2];
            const response = await fetch('/api/tools/' + toolId, {
```

**To:**
```javascript
function toolEditForm(toolId) {
    return {
        toolId: toolId,
        formData: { ... },
        async init() {
            const response = await fetch('/api/tools/' + this.toolId, {
```

### 3. Update PUT Request

**Changed:**
```javascript
const toolId = window.location.pathname.split('/')[2];
const response = await fetch('/api/tools/' + toolId, {
```

**To:**
```javascript
const response = await fetch('/api/tools/' + this.toolId, {
```

## Files Modified

- `src/main/resources/templates/tools/edit.html`

## Testing

After these changes:
- ✅ Edit page loads without template errors
- ✅ Tool data fetched correctly via GET /api/tools/{id}
- ✅ Tool updates work via PUT /api/tools/{id}
- ✅ Delete functionality works via DELETE /api/tools/{id}
- ✅ Test functionality works via POST /api/tools/{id}/validate

## Key Takeaway

When using Qute templates with Alpine.js:
1. Pass Qute variables directly to Alpine.js components as parameters
2. Don't rely on URL parsing when the data is already available from the server
3. Store the passed parameter in the Alpine.js component state for reuse
