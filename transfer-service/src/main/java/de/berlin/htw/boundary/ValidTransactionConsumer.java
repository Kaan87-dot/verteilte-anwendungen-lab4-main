package de.berlin.htw.boundary;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.control.AccountService;
import de.berlin.htw.entity.Account;
import de.berlin.htw.entity.TransactionEntity;
import org.jboss.logging.Logger;

import java.math.BigDecimal;

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

        // IDEMPOTENZ: Prüfen ob Transaktion bereits verarbeitet wurde
        // IDEMPOTENCY: Check if transaction has already been processed
        if (accountService.isTransactionProcessed(tx.getTransactionId())) {
            LOG.warn("Transaction " + tx.getTransactionId() + " already processed. Skipping.");
            return;
        }

        // 1. Konten erstellen falls nicht vorhanden (mit Startguthaben 1000.00)
        // 1. Create accounts if they don't exist (with initial balance 1000.00)
        Account fromAccount = accountService.getOrCreateAccount(tx.getFromAccount());
        Account toAccount = accountService.getOrCreateAccount(tx.getToAccount());

        // 2. Neuen Kontostand berechnen: Betrag von Konto A abziehen
        // 2. Calculate new balance: subtract amount from account A
        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(new BigDecimal(tx.getAmount()));
        accountService.updateBalance(tx.getFromAccount(), fromNewBalance);
        LOG.info("Updated balance for " + tx.getFromAccount() + ": " + fromNewBalance);

        // 3. Neuen Kontostand berechnen: Betrag zu Konto B hinzufügen
        // 3. Calculate new balance: add amount to account B
        BigDecimal toNewBalance = toAccount.getBalance().add(new BigDecimal(tx.getAmount()));
        accountService.updateBalance(tx.getToAccount(), toNewBalance);
        LOG.info("Updated balance for " + tx.getToAccount() + ": " + toNewBalance);

        // 4. Transaktion in der Datenbank speichern
        // 4. Save transaction in the database
        TransactionEntity transactionEntity = new TransactionEntity(
            tx.getTransactionId(),
            tx.getFromAccount(),
            tx.getToAccount(),
            new BigDecimal(tx.getAmount()),
            tx.getCurrency(),
            tx.getTimestamp()
        );
        accountService.saveTransaction(transactionEntity);
        LOG.info("Transaction saved: " + tx.getTransactionId());
    }
}

