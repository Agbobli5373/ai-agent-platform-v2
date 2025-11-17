package com.platform.security;

import com.platform.exception.AuthorizationException;
import com.platform.service.AuthorizationService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@RequiresPermission(value = {})
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class PermissionInterceptor {

    @Inject
    AuthorizationService authorizationService;

    @AroundInvoke
    public Object checkPermission(InvocationContext context) throws Exception {
        RequiresPermission annotation = context.getMethod().getAnnotation(RequiresPermission.class);

        if (annotation == null) {
            annotation = context.getTarget().getClass().getAnnotation(RequiresPermission.class);
        }

        if (annotation != null && annotation.value().length > 0) {
            Permission[] requiredPermissions = annotation.value();

            // Check if user has at least one of the required permissions
            boolean hasPermission = false;
            for (Permission permission : requiredPermissions) {
                if (authorizationService.hasPermission(permission)) {
                    hasPermission = true;
                    break;
                }
            }

            if (!hasPermission) {
                throw new AuthorizationException("Insufficient permissions to access this resource");
            }
        }

        return context.proceed();
    }
}
