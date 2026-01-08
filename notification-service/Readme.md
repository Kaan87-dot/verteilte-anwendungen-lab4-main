# Notification Service

## Beschreibung

Der **Notification Service** konsumiert valid Transaktionen und Fraud Alerts und protokolliert diese mit entsprechenden Log-Levels.

## Konfiguration

- **Port**: 8085
- **Kafka Topics (Incoming)**: `valid-transactions`, `fraud-alerts`
- **Consumer Group**: `notification-group`

## Aufgaben (@TODO im Code)

Der Notification-Service muss jeweils aus den Topics `valid-transactions` und `fraud-alerts` Nachrichten lesen und diese loggen.

- Aus dem `valid-transactions` Topic müssen die Transaktionen mit `LOG.info()` geloggt werden
- Aus dem `fraud-alerts` Topic müssen die Alerts mit `LOG.warn()` geloggt werden

Implementieren Sie den Code dafür in der `NotificationConsumer`-Klasse und konfigurieren Sie die Kanäle (Consumers, Consumer Groups etc.) der Topics in der `application.properties`.
