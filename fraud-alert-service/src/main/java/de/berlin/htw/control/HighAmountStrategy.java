package de.berlin.htw.control;

import de.berlin.htw.boundary.FraudeAlertType;
import de.berlin.htw.boundary.dto.Transaction;


public class HighAmountStrategy implements IFraudStrategy {

    // TODO überschreiben Sie die Methoden Hier

    @Override
    public boolean isFraud(Transaction tx) {
        return false;
    }

    @Override
    public FraudeAlertType getAlertType() {
        return null;
    }
}

