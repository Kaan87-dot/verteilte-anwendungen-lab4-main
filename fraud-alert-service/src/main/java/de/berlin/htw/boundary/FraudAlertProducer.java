package de.berlin.htw.boundary;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import de.berlin.htw.boundary.dto.FraudAlert;
import de.berlin.htw.boundary.dto.Transaction;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class FraudAlertProducer {

    private static final Logger LOG = Logger.getLogger(FraudAlertProducer.class);

    // Injiziert den Emitter für das fraud-alerts Topic
    // Injects the emitter for the fraud-alerts topic
    @Inject
    @Channel("fraud-alerts-out")
    Emitter<FraudAlert> fraudAlertEmitter;

    public void sendAlert(FraudAlert alert) {
        // Sendet den Fraud Alert an das fraud-alerts Kafka Topic
        // Sends the fraud alert to the fraud-alerts Kafka topic
        fraudAlertEmitter.send(alert);
        LOG.warn("Fraud Alert sent: " + alert.getAlertType() + " for account " + alert.getAccountId());
    }

    public void sendFraud(Transaction tx, FraudeAlertType alertType) {
        FraudAlert alert = new FraudAlert();
        alert.setAlertId(UUID.randomUUID().toString());
        alert.setAccountId(tx.getFromAccount());
        alert.setTransactionAmount(tx.getAmount());
        alert.setAlertType(alertType);
        alert.setTimestamp(Instant.now().toString());

        sendAlert(alert);
    }
}

