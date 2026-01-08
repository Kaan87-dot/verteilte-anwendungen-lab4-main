package de.berlin.htw.repository;

import de.berlin.htw.entity.TransactionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<TransactionEntity> {
    
    public TransactionEntity findByTransactionId(String transactionId) {
        return find("transactionId", transactionId).firstResult();
    }
    
    public List<TransactionEntity> findByFromAccount(String fromAccount) {
        return find("fromAccount", fromAccount).list();
    }
    
    public List<TransactionEntity> findByToAccount(String toAccount) {
        return find("toAccount", toAccount).list();
    }
    
    public List<TransactionEntity> findByAccount(String accountId) {
        return find("fromAccount = ?1 OR toAccount = ?1", accountId).list();
    }
}

