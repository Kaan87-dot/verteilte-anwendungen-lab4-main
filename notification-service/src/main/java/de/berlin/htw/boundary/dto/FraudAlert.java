package de.berlin.htw.boundary.dto;

import de.berlin.htw.boundary.FraudeAlertType;

public class FraudAlert {
    private String alertId;
    private String accountId;
    private FraudeAlertType alertType;
    private double transactionAmount;
    private String timestamp;
    private String detectedBy;
    private String severity;

    public FraudAlert() {
    }

    public FraudAlert(String alertId, String accountId, FraudeAlertType alertType, double transactionAmount, String timestamp, String detectedBy, String severity) {
        this.alertId = alertId;
        this.accountId = accountId;
        this.alertType = alertType;
        this.transactionAmount = transactionAmount;
        this.timestamp = timestamp;
        this.detectedBy = detectedBy;
        this.severity = severity;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public FraudeAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(FraudeAlertType alertType) {
        this.alertType = alertType;
    }

    public double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetectedBy() {
        return detectedBy;
    }

    public void setDetectedBy(String detectedBy) {
        this.detectedBy = detectedBy;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}

