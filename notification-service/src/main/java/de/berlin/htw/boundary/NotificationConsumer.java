package de.berlin.htw.boundary;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.enterprise.context.ApplicationScoped;
import de.berlin.htw.boundary.dto.Transaction;
import de.berlin.htw.boundary.dto.FraudAlert;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationConsumer.class);


    // TODO : Hier die Methode implementieren um die Transaktionen aus der Topic "fraud-alerts, valid-transactions" zu konsumieren

}

    

