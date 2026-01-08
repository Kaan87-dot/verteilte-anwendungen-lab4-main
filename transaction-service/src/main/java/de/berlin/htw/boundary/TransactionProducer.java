package de.berlin.htw.boundary;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import io.quarkus.scheduler.Scheduled;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.control.ValidationService;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

@ApplicationScoped
public class TransactionProducer {

    private static final Logger LOG = Logger.getLogger(TransactionProducer.class);

    @Inject
    @Channel("raw-transactions-out")
    Emitter<Transaction> rawEmitter;

    @Inject
    ValidationService validationService;

    private final Random random = new Random();

   //@Scheduled(every = "10s", delay = 0L)
    void produceTransactions() {
        int validCount = 0;
        for (int i = 0; i < 4; i++) {
            Transaction tx = generateRandomTransaction(i);
            if (validationService.isValid(tx)) {
                // send message to topic "raw transactions"
                rawEmitter.send(tx);
                validCount++;
            } else {
                LOG.warn("Invalid transaction filtered: " + tx);
            }
        }
        LOG.info(validCount + " valid transactions sent to raw-transactions!");
    }

    private Transaction generateRandomTransaction(int i) {
        Set<String> countries = Set.of(
            "NG", "KP", "RU", "US", "CA", "MX", "BR", 
            "AU", "IN", "GB", "JP", "KR", "CN", "DE"
        );
        List<String> countryList = new ArrayList<>(countries);

        boolean valid = random.nextBoolean();

        String fromCountry = countryList.get(random.nextInt(countryList.size()));
        String toCountry = countryList.get(random.nextInt(countryList.size()));

        String from = valid 
                ? fromCountry + "Acc" + random.nextInt(3) 
                : "???";
        String to = valid 
                ? toCountry + "Acc" + random.nextInt(3) 
                : "";

        double amount = valid ? (10 + random.nextInt(5000)) : -1.0; 
        String currency = "EUR";

        return new Transaction(from, to, amount, currency);
    }
}

