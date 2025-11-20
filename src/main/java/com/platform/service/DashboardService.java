package com.platform.service;

import com.platform.domain.Conversation;
import com.platform.domain.InteractionMetrics;
import com.platform.repository.AgentRepository;
import com.platform.repository.ConversationRepository;
import com.platform.rest.DashboardResource.ActivityItem;
import com.platform.rest.DashboardResource.DashboardStats;
import com.platform.security.SecurityContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {

    @Inject
    SecurityContext securityContext;

    @Inject
    AgentRepository agentRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Transactional
    public DashboardStats getDashboardStats() {
        UUID organizationId = securityContext.getCurrentOrganizationId();

        if (organizationId == null) {
            return new DashboardStats(0, 0, 0, 0.0);
        }

        // Count total agents for this organization
        long totalAgents = agentRepository.count("organization.id", organizationId);

        // Count total conversations for this organization's agents
        long totalConversations = conversationRepository.count(
                "agent.organization.id", organizationId);

        // Calculate average response time from interaction metrics
        Double avgResponseTime = InteractionMetrics.find(
                "SELECT AVG(im.responseTimeMs) FROM InteractionMetrics im WHERE im.agent.organization.id = ?1",
                organizationId).project(Double.class).firstResult();

        // For now, satisfaction score is placeholder (would need user feedback feature)
        int satisfactionScore = 0;

        return new DashboardStats(
                (int) totalAgents,
                (int) totalConversations,
                satisfactionScore,
                avgResponseTime != null ? avgResponseTime : 0.0);
    }

    @Transactional
    public List<ActivityItem> getRecentActivity() {
        UUID organizationId = securityContext.getCurrentOrganizationId();

        if (organizationId == null) {
            return new ArrayList<>();
        }

        // Get recent conversations for this organization
        List<Conversation> recentConversations = conversationRepository.find(
                "agent.organization.id = ?1 ORDER BY createdAt DESC",
                organizationId).page(0, 10).list();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return recentConversations.stream()
                .map(conv -> new ActivityItem(
                        conv.id.toString(),
                        "conversation",
                        "New conversation with " + (conv.agent != null ? conv.agent.name : "agent"),
                        conv.createdAt != null ? conv.createdAt.format(formatter)
                                : LocalDateTime.now().format(formatter)))
                .collect(Collectors.toList());
    }
}
