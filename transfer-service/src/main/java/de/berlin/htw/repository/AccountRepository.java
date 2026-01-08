package de.berlin.htw.repository;

import de.berlin.htw.entity.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AccountRepository implements PanacheRepository<Account> {
    
    public Account findByAccountId(String accountId) {
        return find("accountId", accountId).firstResult();
    }
}

