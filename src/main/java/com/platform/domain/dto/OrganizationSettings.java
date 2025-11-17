package com.platform.domain.dto;

/**
 * Organization-level settings and preferences
 */
public class OrganizationSettings {
    public Boolean enableNotifications;
    public String timezone;
    public String defaultLanguage;
    public Boolean enableUsageAlerts;
    public Integer alertThresholdPercentage;

    public OrganizationSettings() {
    }

    public OrganizationSettings(Boolean enableNotifications, String timezone, String defaultLanguage,
            Boolean enableUsageAlerts, Integer alertThresholdPercentage) {
        this.enableNotifications = enableNotifications;
        this.timezone = timezone;
        this.defaultLanguage = defaultLanguage;
        this.enableUsageAlerts = enableUsageAlerts;
        this.alertThresholdPercentage = alertThresholdPercentage;
    }
}
