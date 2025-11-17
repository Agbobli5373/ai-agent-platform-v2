package com.platform.service;

import com.platform.domain.Organization;
import com.platform.domain.User;
import com.platform.exception.ValidationException;
import com.platform.repository.UserRepository;
import com.platform.security.PasswordHasher;
import com.platform.security.Role;
import com.platform.security.dto.RegistrationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class RegistrationService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordHasher passwordHasher;

    /**
     * Register a new user with a new organization
     */
    @Transactional
    public User registerUser(RegistrationRequest request) {
        // Validate input
        if (request == null || request.email == null || request.email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (request.password == null || request.password.isBlank()) {
            throw new ValidationException("Password is required");
        }
        if (request.organizationName == null || request.organizationName.isBlank()) {
            throw new ValidationException("Organization name is required");
        }

        // Normalize email to lowercase
        String normalizedEmail = request.email.trim().toLowerCase();

        // Check if email already exists
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ValidationException("Email already registered");
        }

        // Create organization
        Organization organization = new Organization();
        organization.name = request.organizationName;
        organization.createdAt = LocalDateTime.now();
        organization.persist();

        // Create user
        User user = new User();
        user.email = normalizedEmail;
        user.passwordHash = passwordHasher.hashPassword(request.password);
        user.organization = organization;
        user.role = Role.ADMIN.name();
        user.createdAt = LocalDateTime.now();
        user.persist();

        return user;
    }
}
