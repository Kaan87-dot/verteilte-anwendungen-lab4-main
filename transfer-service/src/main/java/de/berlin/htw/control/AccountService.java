package de.berlin.htw.control;

import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.entity.Account;
import de.berlin.htw.entity.TransactionEntity;
import de.berlin.htw.repository.AccountRepository;
import de.berlin.htw.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);

    @Inject
    AccountRepository accountRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Account getOrCreateAccount(String accountId) {
        // Try to find existing account first
        Account account = accountRepository.findByAccountId(accountId);
        if (account != null) {
            return account;
        }
        
        // Account doesn't exist, try to create it
        // If another transaction created it simultaneously, the UNIQUE constraint
        // will prevent duplicates and we'll fetch it in the catch block
        try {
            account = new Account(accountId, new BigDecimal("1000.00"));
            accountRepository.persist(account);
            accountRepository.flush(); // Force immediate write to detect constraint violations
            return account;
        } catch (Exception e) {
            // If constraint violation (duplicate account_id), fetch the existing account
            // This handles race conditions where multiple consumers try to create the same account
            Account existingAccount = accountRepository.findByAccountId(accountId);
            if (existingAccount != null) {
                LOG.debug("Account " + accountId + " was created by another transaction, using existing account");
                return existingAccount;
            }
            // If still null, rethrow the original exception
            throw e;
        }
    }

    public double getBalance(String accountId) {
        Account account = accountRepository.findByAccountId(accountId);
        return account != null ? account.getBalance().doubleValue() : 0.0;
    }

    @Transactional
    public void updateBalance(String accountId, BigDecimal newBalance) {
        Account account = accountRepository.findByAccountId(accountId);
        if (account != null) {
            account.setBalance(newBalance);
            accountRepository.persist(account);
        }
    }

    @Transactional
    public void saveTransaction(TransactionEntity transaction) {
        transactionRepository.persist(transaction);
    }

    // Idempotenz-Check: Prüft ob eine Transaktion bereits verarbeitet wurde
    // Idempotency check: Checks if a transaction has already been processed
    public boolean isTransactionProcessed(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId) != null;
    }

    public List<Transaction> getTransactionsFromAccount(String accountId) {
        return transactionRepository.findByFromAccount(accountId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsToAccount(String accountId) {
        return transactionRepository.findByToAccount(accountId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactionsForAccount(String accountId) {
        return transactionRepository.findByAccount(accountId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.listAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Transaction toDTO(TransactionEntity entity) {
        Transaction tx = new Transaction();
        tx.setTransactionId(entity.getTransactionId());
        tx.setFromAccount(entity.getFromAccount());
        tx.setToAccount(entity.getToAccount());
        tx.setAmount(entity.getAmount().doubleValue());
        tx.setCurrency(entity.getCurrency());
        tx.setTimestamp(entity.getTimestamp());
        return tx;
    }
}
