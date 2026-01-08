package de.berlin.htw.boundary.dto;

import java.time.Instant;
import java.util.UUID;

public class Transaction {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private double amount;
    private String currency;
    private String timestamp;

    public Transaction() {
    }

    public Transaction(String fromAccount, String toAccount, double amount, String currency) {
        this.transactionId = UUID.randomUUID().toString();
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = Instant.now().toString();
    }

    public Transaction(String transactionId, String fromAccount, String toAccount, double amount, String currency, String timestamp) {
        this.transactionId = transactionId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

