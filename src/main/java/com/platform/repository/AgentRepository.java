package com.platform.repository;

import com.platform.domain.Agent;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AgentRepository implements PanacheRepository<Agent> {
}
