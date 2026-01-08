package de.berlin.htw.control;

import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.entity.Account;
import de.berlin.htw.entity.TransactionEntity;
import de.berlin.htw.repository.AccountRepository;
import de.berlin.htw.repository.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccountService {

    @Inject
    AccountRepository accountRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Transactional
    public Account getOrCreateAccount(String accountId) {
        Account account = accountRepository.findByAccountId(accountId);
        if (account == null) {
            account = new Account(accountId, new BigDecimal("1000.00"));
            accountRepository.persist(account);
        }
        return account;
    }

    public double getBalance(String accountId) {
        Account account = accountRepository.findByAccountId(accountId);
        return account != null ? account.getBalance().doubleValue() : 0.0;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void updateBalance(String accountId, BigDecimal newBalance) {
        Account account = getOrCreateAccount(accountId);
        account.setBalance(newBalance);
    }

    @Transactional
    public void saveTransaction(TransactionEntity transaction) {
        transactionRepository.persist(transaction);
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
