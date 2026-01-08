package de.berlin.htw.boundary;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import de.berlin.htw.boundary.dto.Transaction;
import io.smallrye.reactive.messaging.kafka.Record;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ValidTransactionProducer {

    private static final Logger LOG = Logger.getLogger(ValidTransactionProducer.class);

    @Inject
    @Channel("valid-transactions-out")
    Emitter<Record<String, Transaction>> producer;

    public void sendValidTransaction(Transaction tx) {
        producer.send(Record.of(tx.getFromAccount(), tx));
        LOG.info("Valid Transaction sent: " + tx);
    }
}

