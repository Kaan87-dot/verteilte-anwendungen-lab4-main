package de.berlin.htw.boundary;

import jakarta.enterprise.context.ApplicationScoped;
import de.berlin.htw.boundary.dto.FraudAlert;
import de.berlin.htw.boundary.dto.Transaction;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class FraudAlertProducer {

    public void sendAlert(FraudAlert alert) {
         // TODO : alert muss dann in der topic fraud-alerts geschrieben werden
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

