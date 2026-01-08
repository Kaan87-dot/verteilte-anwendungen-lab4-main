package de.berlin.htw.boundary;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.control.AccountService;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ValidTransactionConsumer {

    private static final Logger LOG = Logger.getLogger(ValidTransactionConsumer.class);

    @Inject
    AccountService accountService;


    @Incoming("valid-transactions-in")
    @Transactional
    public void processValid(Transaction tx) {
        String consumerGroup = System.getenv("KAFKA_CONSUMER_GROUP_ID");
        LOG.info("[" + consumerGroup + "] Received transaction: " + tx.getTransactionId() + 
                 " from " + tx.getFromAccount() + " to " + tx.getToAccount() + 
                 " amount: " + tx.getAmount());

        // @TODO : here

    }
}

