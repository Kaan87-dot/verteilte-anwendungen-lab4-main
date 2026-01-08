package de.berlin.htw.control;

import java.util.Set;
import de.berlin.htw.boundary.FraudeAlertType;
import de.berlin.htw.boundary.dto.Transaction;

public class LocationStrategy implements IFraudStrategy {

    private final Set<String> suspiciousCountries = Set.of("NG", "KP", "RU");

    @Override
    public boolean isFraud(Transaction tx) {
        for (String sc: suspiciousCountries) {
            if (tx.getFromAccount().contains(sc) || tx.getToAccount().contains(sc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FraudeAlertType getAlertType() {
        return FraudeAlertType.SUSPICIOUS_LOCATION;
    }
}

