package com.platform.repository;

import com.platform.domain.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }

    public Optional<User> findByIdWithOrganization(UUID id) {
        return find("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.id = ?1", id)
                .firstResultOptional();
    }

    public Optional<User> findByEmailAndOrganization(String email, UUID organizationId) {
        return find("email = ?1 AND organization.id = ?2", email, organizationId)
                .firstResultOptional();
    }

    public List<User> findByOrganization(UUID organizationId) {
        return find("organization.id", organizationId).list();
    }

    public long countByOrganization(UUID organizationId) {
        return count("organization.id", organizationId);
    }

    public List<User> findByRole(String role) {
        return find("role", role).list();
    }

    public List<User> findByOrganizationAndRole(UUID organizationId, String role) {
        return find("organization.id = ?1 AND role = ?2", organizationId, role).list();
    }
}
