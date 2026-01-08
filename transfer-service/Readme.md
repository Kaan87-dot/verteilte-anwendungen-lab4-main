# Transfer Service

## Beschreibung

Der **Transfer Service** empfängt valid Transaktionen aus dem Kafka-Topic `valid-transactions` und führt die Geldüberweisungen durch. Kontostände und Transaktionen werden in der PostgreSQL-Datenbank gespeichert.

## Konfiguration

- **Port**: 8084, 8086, 8087 (mehrere Instanzen möglich)
- **Kafka Topic (Incoming)**: `valid-transactions`
- **Consumer Group**: Konfigurierbar über `KAFKA_CONSUMER_GROUP_ID`
- **Datenbank**: PostgreSQL (Tabellen: `accounts`, `transactions`)

## Aufgaben (@TODO im Code)

### Transaktionsverarbeitung implementieren
**Datei**: `transfer-service/src/main/java/de/berlin/htw/boundary/ValidTransactionConsumer.java`

Vervollständigen Sie die Methode `processValid()`:

1. Den `AccountService` benutzen, um ein Konto zu erstellen, wenn es nicht existiert, und dieses mit 1000.00 zu füllen. Die Methode gibt es bereits und muss nur aus dem `AccountService` aufgerufen werden.

2. Das Geld von Konto A abziehen und zu Konto B hinzufügen. Dafür das `updateBalance` aus dem `AccountService` nutzen.

3. Die Transaktion auch in die `transactions` Tabelle speichern. Die Tabellen sind bereits vorimplementiert.

4. Konfigurieren Sie die Consumer Groups, Topics etc. in der `application.properties`.

Nutzen Sie die verfügbaren Methoden des `AccountService` für die Implementierung.
