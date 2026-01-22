package de.berlin.htw.control;

import de.berlin.htw.boundary.FraudeAlertType;
import de.berlin.htw.boundary.dto.Transaction;


public class HighAmountStrategy implements IFraudStrategy {

    // Schwellenwert für hohe Beträge
    // Threshold for high amounts
    private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;

    @Override
    public boolean isFraud(Transaction tx) {
        // Prüft ob der Transaktionsbetrag über 10000 liegt
        // Checks if the transaction amount is greater than 10000
        return tx.getAmount() > HIGH_AMOUNT_THRESHOLD;
    }

    @Override
    public FraudeAlertType getAlertType() {
        // Gibt den Typ des Fraud-Alerts zurück (HIGH_AMOUNT)
        // Returns the type of fraud alert (HIGH_AMOUNT)
        return FraudeAlertType.HIGH_AMOUNT;
    }
}

