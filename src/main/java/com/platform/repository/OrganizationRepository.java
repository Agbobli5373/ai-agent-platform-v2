package com.platform.repository;

import com.platform.domain.Organization;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class OrganizationRepository implements PanacheRepositoryBase<Organization, UUID> {

    public Organization findByName(String name) {
        return find("name", name).firstResult();
    }
}
