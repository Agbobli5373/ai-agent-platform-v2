package com.platform.repository;

import com.platform.domain.Conversation;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConversationRepository implements PanacheRepository<Conversation> {
}
