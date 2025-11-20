package com.platform.repository;

import com.platform.domain.Agent;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class AgentRepository implements PanacheRepositoryBase<Agent, UUID> {
}
