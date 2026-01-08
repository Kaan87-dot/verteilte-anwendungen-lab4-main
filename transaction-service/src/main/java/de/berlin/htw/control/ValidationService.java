package de.berlin.htw.control;

import de.berlin.htw.boundary.dto.Transaction;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ValidationService {

    public boolean isValid(Transaction tx) {
        if (tx.getFromAccount() == null || tx.getFromAccount().isBlank()) return false;
        if (tx.getToAccount() == null || tx.getToAccount().isBlank()) return false;
        if (tx.getAmount() <= 0) return false;
        return true;
    }
}

