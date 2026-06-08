package com.circleguard.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Feature Toggle pattern — controls runtime behavior without code deployments.
 * Values are read from application.yml and can be overridden per-environment
 * via Kubernetes ConfigMaps (circleguard-config).
 *
 * Usage: inject FeatureFlags and check isXxx() before executing the feature.
 */
@Component
@ConfigurationProperties(prefix = "circleguard.features")
public class FeatureFlags {

    /**
     * When true, Redis unavailability causes access denial (deny-on-uncertainty).
     * When false, Redis unavailability allows access (allow-on-uncertainty).
     * Default: true (safe for production).
     */
    private boolean strictQrValidation = true;

    /**
     * When true, email notifications are sent for health status changes.
     * Can be disabled during maintenance or high-load periods.
     */
    private boolean emailNotificationsEnabled = true;

    /**
     * When true, QR tokens are validated with a 5-minute expiration window.
     * When false, tokens are valid for the entire day (useful for demos/testing).
     */
    private boolean qrExpirationEnabled = true;

    public boolean isStrictQrValidation() { return strictQrValidation; }
    public void setStrictQrValidation(boolean strictQrValidation) { this.strictQrValidation = strictQrValidation; }

    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public boolean isQrExpirationEnabled() { return qrExpirationEnabled; }
    public void setQrExpirationEnabled(boolean qrExpirationEnabled) { this.qrExpirationEnabled = qrExpirationEnabled; }
}
