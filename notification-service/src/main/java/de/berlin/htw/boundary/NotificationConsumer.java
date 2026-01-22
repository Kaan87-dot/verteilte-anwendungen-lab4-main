package de.berlin.htw.boundary;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.boundary.dto.FraudAlert;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationConsumer.class);

    // Konsumiert gültige Transaktionen aus dem valid-transactions Topic
    // Consumes valid transactions from the valid-transactions topic
    @Incoming("valid-transactions-in")
    public void consumeValidTransaction(Transaction tx) {
        // Loggt gültige Transaktionen mit INFO Level
        // Logs valid transactions with INFO level
        LOG.info("✓ Valid Transaction: " + tx.getTransactionId() + 
                 " | From: " + tx.getFromAccount() + 
                 " | To: " + tx.getToAccount() + 
                 " | Amount: " + tx.getAmount() + " " + tx.getCurrency());
    }

    // Konsumiert Fraud Alerts aus dem fraud-alerts Topic
    // Consumes fraud alerts from the fraud-alerts topic
    @Incoming("fraud-alerts-in")
    public void consumeFraudAlert(FraudAlert alert) {
        // Loggt Fraud Alerts mit WARNING Level
        // Logs fraud alerts with WARNING level
        LOG.warn("⚠ FRAUD ALERT: " + alert.getAlertType() + 
                 " | Alert ID: " + alert.getAlertId() + 
                 " | Account: " + alert.getAccountId() + 
                 " | Amount: " + alert.getTransactionAmount() + 
                 " | Timestamp: " + alert.getTimestamp());
    }
}

    

