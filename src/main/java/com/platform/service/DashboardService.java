package com.platform.service;

import com.platform.rest.DashboardResource.ActivityItem;
import com.platform.rest.DashboardResource.DashboardStats;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DashboardService {

    public DashboardStats getDashboardStats() {
        // TODO: Implement actual stats calculation from database
        // For now, return mock data
        return new DashboardStats(
                0, // totalAgents
                0, // totalConversations
                0, // satisfactionScore
                0.0 // avgResponseTime
        );
    }

    public List<ActivityItem> getRecentActivity() {
        // TODO: Implement actual activity retrieval from database
        // For now, return empty list
        return new ArrayList<>();
    }
}
