package com.platform.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Exception occurred", exception);

        if (exception instanceof AuthenticationException) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("AUTHENTICATION_ERROR", exception.getMessage()))
                    .build();
        }

        if (exception instanceof AuthorizationException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse("AUTHORIZATION_ERROR", exception.getMessage()))
                    .build();
        }

        if (exception instanceof ValidationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("VALIDATION_ERROR", exception.getMessage()))
                    .build();
        }

        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) exception;
            Map<String, String> errors = cve.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            cv -> cv.getPropertyPath().toString(),
                            ConstraintViolation::getMessage,
                            (existing, replacement) -> existing));
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ValidationErrorResponse("VALIDATION_ERROR", "Validation failed", errors))
                    .build();
        }

        // Default error response
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"))
                .build();
    }

    public static class ErrorResponse {
        public String code;
        public String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static class ValidationErrorResponse extends ErrorResponse {
        public Map<String, String> fieldErrors;

        public ValidationErrorResponse(String code, String message, Map<String, String> fieldErrors) {
            super(code, message);
            this.fieldErrors = fieldErrors;
        }
    }
}
