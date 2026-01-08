package de.berlin.htw.control;

import de.berlin.htw.boundary.FraudeAlertType;
import de.berlin.htw.boundary.dto.Transaction;

public interface IFraudStrategy {
    boolean isFraud(Transaction tx);
    FraudeAlertType getAlertType();
}

