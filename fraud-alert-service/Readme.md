# Fraud Alert Service

## Beschreibung

Der **Fraud Alert Service** empfängt Transaktionen aus dem Kafka-Topic `raw-transactions` und prüft diese mithilfe des **Strategy Patterns** auf möglichen Fraud. Erkannte Fraud-Fälle werden an das Topic `fraud-alerts` gesendet, valid Transaktionen an `valid-transactions`.

## Konfiguration

- **Port**: 8083
- **Kafka Topics (Incoming)**: `raw-transactions`
- **Kafka Topics (Outgoing)**: `fraud-alerts`, `valid-transactions`
- **Consumer Group**: `fraud-check-group`

## Aufgaben (@TODO im Code)

### 1. HighAmountStrategy implementieren
**Datei**: `fraud-alert-service/src/main/java/de/berlin/htw/control/HighAmountStrategy.java`

Der Fraud Alert Service prüft alle Transaktionen, die aus dem `raw-transactions` Topic kommen, ob diese Fraud sind. Dafür nutzt er das Strategy Pattern. Schauen Sie, wie das funktioniert. Sie müssen die Methode in `HighAmountStrategy` implementieren und diese auch im `FraudCheckService` registrieren. Anhand dieser Strategie könnte man jeweils mehrere Fraud-Check-Methoden weiter hinzufügen und hat dadurch schlankeren Code. Orientieren Sie sich an der `LocationStrategy` und wie bzw. wo diese registriert ist.

- Implementieren Sie die Strategie, die Transaktionen mit Beträgen über **10.000** als Fraud erkennt
- Registrieren Sie die neue Strategie im Konstruktor des `FraudCheckService`

### 2. FraudAlertProducer implementieren
**Datei**: `fraud-alert-service/src/main/java/de/berlin/htw/boundary/FraudAlertProducer.java`

Anschließend schreibt dieser Microservice alle Fraud-Fälle, die er gefunden hat, in das `fraud-alerts` Topic, und die, die nicht als Fraud erkannt werden, werden in das `valid-transactions` Topic geschrieben. Implementieren Sie den Code im `FraudAlertProducer` und im `ValidTransactionProducer`. Die Konfiguration der Topics, Consumer Groups etc. müssen Sie dann wie immer in der `application.properties` auch mitkonfigurieren.
