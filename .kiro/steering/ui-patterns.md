# UI Patterns and Guidelines

## Qute Template Structure

All page templates should follow this standard structure:

### 1. Include Dashboard Layout
```html
{#include layout/dashboard.html}
```

### 2. Define Required Sections

#### Title
```html
{#title}Page Name - AI Agent Platform{/title}
```

#### User Information (for dashboard layout)
```html
{#userInitials}{userInitials}{/userInitials}
{#userName}{userName}{/userName}
{#userRole}{userRole}{/userRole}
{#profileUserName}{userName}{/profileUserName}
{#profileUserEmail}{userEmail}{/profileUserEmail}
{#currentOrgName}{currentOrgName}{/currentOrgName}
```

#### Breadcrumbs
```html
{#breadcrumbs}
<a href="/dashboard" class="text-gray-500 hover:text-gray-700">Dashboard</a>
<span class="text-gray-400 mx-2">/</span>
<span class="text-gray-900 font-medium">Current Page</span>
{/breadcrumbs}
```

#### Content
```html
{#content}
<div x-data="pageFunction()">
    <!-- Page content here -->
</div>
{/content}
```

#### Scripts
```html
{#scripts}
<script>
function pageFunction() {
    return {
        // Alpine.js component data and methods
        async init() {
            // Check authentication
            const token = localStorage.getItem('accessToken');
            if (!token) {
                window.location.href = '/auth/login';
                return;
            }
            
            // Load data
            await this.loadData();
        },
        
        async loadData() {
            // API calls with JWT token
        }
    }
}
</script>
{/scripts}
```

## JWT Authentication Pattern

### Storing Tokens
Tokens are stored in localStorage:
- `accessToken` - JWT access token
- `refreshToken` - Refresh token (if applicable)

### API Request Pattern
All API requests must include the JWT token in the Authorization header:

```javascript
const token = localStorage.getItem('accessToken');
const response = await fetch('/api/endpoint', {
    method: 'GET', // or POST, PUT, DELETE
    headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json' // for JSON requests
    },
    body: JSON.stringify(data) // for POST/PUT requests
});

if (response.ok) {
    const data = await response.json();
    // Handle success
} else if (response.status === 401) {
    // Token expired or invalid
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    window.location.href = '/auth/login';
} else {
    // Handle other errors
}
```

### File Upload Pattern
For multipart form data (file uploads):

```javascript
const token = localStorage.getItem('accessToken');
const formData = new FormData();
formData.append('file', fileObject);
formData.append('fileName', fileObject.name);
formData.append('contentType', fileObject.type);

const response = await fetch('/api/documents/upload', {
    method: 'POST',
    headers: {
        'Authorization': 'Bearer ' + token
        // Do NOT set Content-Type for FormData - browser sets it automatically
    },
    body: formData
});
```

## Alpine.js Component Pattern

### Component Structure
```javascript
function componentName() {
    return {
        // Data properties
        items: [],
        loading: false,
        errorMessage: '',
        
        // Lifecycle
        async init() {
            await this.checkAuth();
            await this.loadData();
        },
        
        // Methods
        async checkAuth() {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                window.location.href = '/auth/login';
                return false;
            }
            return true;
        },
        
        async loadData() {
            try {
                this.loading = true;
                const token = localStorage.getItem('accessToken');
                const response = await fetch('/api/endpoint', {
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                
                if (response.ok) {
                    this.items = await response.json();
                } else if (response.status === 401) {
                    localStorage.removeItem('accessToken');
                    window.location.href = '/auth/login';
                }
            } catch (error) {
                console.error('Error:', error);
                this.errorMessage = 'Failed to load data';
            } finally {
                this.loading = false;
            }
        }
    }
}
```

## Tailwind CSS Styling Guidelines

### Color Scheme
- Primary: `indigo-600` (buttons, links, highlights)
- Success: `green-600` (success states, positive indicators)
- Warning: `yellow-600` (warnings, pending states)
- Error: `red-600` (errors, delete actions)
- Gray: `gray-50` to `gray-900` (backgrounds, text, borders)

### Common Component Classes

#### Buttons
```html
<!-- Primary Button -->
<button class="px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700">
    Button Text
</button>

<!-- Secondary Button -->
<button class="px-4 py-2 border border-gray-300 rounded-md text-gray-700 bg-white hover:bg-gray-50">
    Button Text
</button>

<!-- Danger Button -->
<button class="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700">
    Delete
</button>
```

#### Cards
```html
<div class="bg-white shadow rounded-lg p-6">
    <!-- Card content -->
</div>
```

#### Status Badges
```html
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
    Active
</span>
```

#### Form Inputs
```html
<input type="text" 
       class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
       placeholder="Enter text">
```

## Template Naming Convention

Templates should be named using snake_case and placed in appropriate directories:

- Page templates: `templates/{feature}_{page}.html`
  - Example: `documents_list.html`, `documents_upload.html`
  
- Layout templates: `templates/layout/{name}.html`
  - Example: `layout/dashboard.html`, `layout/base.html`

## Resource Injection Pattern

In Quarkus REST resources, inject templates using snake_case names:

```java
@Inject
Template documents_list;  // matches templates/documents_list.html

@Inject
Template documents_upload; // matches templates/documents_upload.html
```

## Error Handling

### Display Error Messages
```html
<div x-show="errorMessage" class="p-4 bg-red-50 rounded-lg">
    <div class="flex">
        <svg class="h-5 w-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="ml-3 text-sm text-red-700" x-text="errorMessage"></p>
    </div>
</div>
```

### Loading States
```html
<div x-show="loading" class="flex justify-center py-8">
    <svg class="animate-spin h-8 w-8 text-indigo-600" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
    </svg>
</div>
```

## Responsive Design

All pages should be mobile-responsive using Tailwind's responsive prefixes:

```html
<!-- Grid that stacks on mobile, 2 columns on tablet, 4 on desktop -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
    <!-- Content -->
</div>

<!-- Hide on mobile, show on desktop -->
<div class="hidden lg:block">
    <!-- Desktop-only content -->
</div>

<!-- Show on mobile, hide on desktop -->
<div class="lg:hidden">
    <!-- Mobile-only content -->
</div>
```

## Common Utility Functions

### Format File Size
```javascript
formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
```

### Format Date
```javascript
formatDate(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now - date;
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 60) return minutes + ' minutes ago';
    if (hours < 24) return hours + ' hours ago';
    if (days < 7) return days + ' days ago';
    return date.toLocaleDateString();
}
```

## Best Practices

1. **Always check authentication** in the `init()` method of Alpine.js components
2. **Handle 401 responses** by clearing tokens and redirecting to login
3. **Use consistent error handling** with try-catch blocks
4. **Show loading states** during async operations
5. **Provide user feedback** for all actions (success/error messages)
6. **Follow mobile-first design** using Tailwind responsive classes
7. **Use semantic HTML** with proper ARIA labels for accessibility
8. **Keep components focused** - one component per page/feature
9. **Reuse common patterns** from existing pages (dashboard, tools, documents)
10. **Test with and without authentication** to ensure proper redirects
