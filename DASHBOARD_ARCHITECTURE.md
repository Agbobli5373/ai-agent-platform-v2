# Dashboard Architecture

## Overview

The dashboard uses a hybrid rendering approach that separates static HTML rendering from dynamic data loading for better performance and scalability.

## Architecture Pattern

### Server-Side Rendering (SSR)
- **Endpoint**: `GET /dashboard`
- **Authentication**: Public access (`@PermitAll`)
- **Response**: Static HTML with placeholder data
- **Template Engine**: Qute
- **Styling**: Tailwind CSS

### Client-Side Data Loading
- **Authentication**: JWT token from localStorage
- **API Endpoints**:
  - `GET /api/auth/me` - User information
  - `GET /dashboard/stats` - Dashboard statistics
  - `GET /dashboard/activity` - Recent activity feed
- **Framework**: Alpine.js for reactivity

## Request Flow

```
1. User navigates to /dashboard
   ↓
2. Server returns static HTML (no authentication required)
   ↓
3. Browser loads HTML with placeholder data
   ↓
4. Alpine.js init() runs on page load
   ↓
5. JavaScript checks localStorage for JWT token
   ↓
6. If no token → Redirect to /auth/login
   ↓
7. If token exists → Fetch user data from /api/auth/me
   ↓
8. Fetch dashboard stats from /dashboard/stats
   ↓
9. Fetch recent activity from /dashboard/activity
   ↓
10. Update UI with real data
```

## Benefits

### Performance
- **Fast Initial Load**: No server-side database queries for page render
- **Better Caching**: Static HTML can be cached aggressively
- **Reduced Server Load**: Authentication and data fetching happen in parallel

### Scalability
- **Stateless Rendering**: Server doesn't need to maintain session state
- **CDN-Friendly**: Static HTML can be served from CDN
- **API-First**: Data endpoints can be scaled independently

### Developer Experience
- **Separation of Concerns**: Clear boundary between presentation and data
- **Easier Testing**: Template rendering and data fetching can be tested separately
- **Flexibility**: Easy to add new data sources without changing template

## Security

### Authentication Flow
1. User logs in via `/api/auth/login`
2. Server returns JWT access token (24h) and refresh token (7d)
3. Client stores tokens in localStorage
4. All API requests include `Authorization: Bearer <token>` header
5. Server validates token and enforces organization-level isolation

### Authorization
- Dashboard HTML page: Public access (no authentication)
- Dashboard API endpoints: Require authentication (`@RolesAllowed`)
- Data isolation: Enforced at API layer by organization_id

## Code Structure

### DashboardResource.java
```java
@Path("/dashboard")
public class DashboardResource {
    
    // Renders static HTML with placeholder data
    @GET
    @PermitAll
    public TemplateInstance home() {
        return dashboardHome
            .data("userInitials", "U")
            .data("userName", "User")
            .data("userEmail", "user@example.com")
            .data("userRole", "USER")
            .data("currentOrgName", "My Organization");
    }
    
    // Returns real dashboard statistics
    @GET
    @Path("/stats")
    @RolesAllowed({ "USER", "ADMIN" })
    public DashboardStats getStats() {
        return dashboardService.getDashboardStats();
    }
    
    // Returns recent activity feed
    @GET
    @Path("/activity")
    @RolesAllowed({ "USER", "ADMIN" })
    public List<ActivityItem> getRecentActivity() {
        return dashboardService.getRecentActivity();
    }
}
```

### dashboard/home.html (Alpine.js)
```javascript
function dashboardHome() {
    return {
        stats: { /* ... */ },
        recentActivity: [],
        
        async init() {
            const token = localStorage.getItem('accessToken');
            if (!token) {
                window.location.href = '/auth/login';
                return;
            }
            
            await this.loadUserInfo();
            await this.loadStats();
            await this.loadRecentActivity();
        },
        
        async loadStats() {
            const response = await fetch('/dashboard/stats', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            this.stats = await response.json();
        }
    }
}
```

## Migration Notes

### Previous Implementation
- Dashboard endpoint required authentication (`@RolesAllowed`)
- User data fetched from database on every page load
- SecurityContext injected to get current user
- Helper methods to format user data (getUserInitials, getUserName)

### Current Implementation
- Dashboard endpoint is public (`@PermitAll`)
- Static HTML with placeholder data
- User data loaded client-side via JavaScript
- Helper methods removed (no longer needed)

### Breaking Changes
- None - API endpoints remain unchanged
- Client-side code handles authentication transparently

## Future Enhancements

1. **Server-Side Rendering (SSR) with Hydration**
   - Render initial data on server for SEO
   - Hydrate with client-side JavaScript for interactivity

2. **Progressive Enhancement**
   - Basic functionality without JavaScript
   - Enhanced experience with JavaScript enabled

3. **Real-Time Updates**
   - WebSocket connection for live dashboard updates
   - Push notifications for alerts and activity

4. **Offline Support**
   - Service worker for offline page access
   - Local caching of dashboard data

## Related Files

- `src/main/java/com/platform/rest/DashboardResource.java` - REST endpoints
- `src/main/java/com/platform/service/DashboardService.java` - Business logic
- `src/main/resources/templates/layout/dashboard.html` - Base layout template
- `src/main/resources/templates/dashboard/home.html` - Home page template
- `README.md` - Project documentation
