package de.berlin.htw.boundary;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.control.FraudCheckService;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FraudCheckConsumer {

    private static final Logger LOG = Logger.getLogger(FraudCheckConsumer.class);

    @Inject
    FraudCheckService fraudCheckService;

    @Incoming("raw-transactions-in")
    public void checkFraud(Transaction tx) {
        try {
            fraudCheckService.checkFraud(tx);
        } catch (Exception e) {
            LOG.error("Failed to process transaction for fraud check", e);
        }
    }
}

