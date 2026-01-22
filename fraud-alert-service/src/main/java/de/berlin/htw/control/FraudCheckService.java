package de.berlin.htw.control;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.boundary.FraudAlertProducer;
import de.berlin.htw.boundary.ValidTransactionProducer;

@ApplicationScoped
public class FraudCheckService {

    @Inject
    FraudAlertProducer fraudAlertProducer;

    @Inject
    ValidTransactionProducer validTxProducer;

    private final List<IFraudStrategy> strategies = new ArrayList<>();

    public FraudCheckService() {
        // Registrierung der Location-Strategie zur Prüfung verdächtiger Länder
        // Registration of Location strategy to check suspicious countries
        strategies.add(new LocationStrategy());
        
        // Registrierung der HighAmount-Strategie zur Prüfung hoher Beträge
        // Registration of HighAmount strategy to check high amounts
        strategies.add(new HighAmountStrategy());
    }

    public void checkFraud(Transaction tx) {
        for (IFraudStrategy strategy : strategies) {
            if (strategy.isFraud(tx)) {
                fraudAlertProducer.sendFraud(tx, strategy.getAlertType());
                return;
            }
        }
        validTxProducer.sendValidTransaction(tx);
    }
}

