package com.platform.repository;

import com.platform.domain.Tool;
import com.platform.domain.User;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ToolRepository implements PanacheRepositoryBase<Tool, UUID> {

    public List<Tool> findByOwner(User owner) {
        return list("owner", owner);
    }

    public List<Tool> findByOrganization(UUID organizationId) {
        return list("owner.organization.id", organizationId);
    }

    public Optional<Tool> findByIdAndOwner(UUID id, User owner) {
        return find("id = ?1 and owner = ?2", id, owner).firstResultOptional();
    }

    public List<Tool> findByType(Tool.ToolType type) {
        return list("type", type);
    }

    public List<Tool> findByNamePattern(String namePattern) {
        return list("LOWER(name) LIKE LOWER(?1)", "%" + namePattern + "%");
    }
}
