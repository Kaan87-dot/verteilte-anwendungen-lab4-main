# Implementierungs-Dokumentation - Verteilte Anwendungen Lab 4

## Überblick
Diese Dokumentation erklärt alle implementierten Änderungen für die Übungsaufgabe 4 und begründet die getroffenen Entscheidungen bezüglich Kafka-Konfiguration, Skalierung und Idempotenz.

---

## Aufgabe 1: Topic-Erstellung raw-transactions

### Konfiguration
- **Topic Name**: `raw-transactions`
- **Anzahl der Partitionen**: 3 (konfiguriert in docker-compose.yml: `KAFKA_NUM_PARTITIONS: 3`)
- **Retention-Zeit**: Standard (7 Tage)

### Begründung der Konfiguration

#### Partitionen (3)
- **Skalierbarkeit**: 3 Partitionen ermöglichen parallele Verarbeitung durch den Fraud-Alert-Service
- **Load Balancing**: Nachrichten werden gleichmäßig auf 3 Partitionen verteilt
- **Zukunftssicherheit**: Falls später mehr Consumer hinzugefügt werden sollen, sind bereits genügend Partitionen vorhanden

#### Retention-Zeit (Standard 7 Tage)
- **Ausreichend für Replay**: Im Fehlerfall können Transaktionen innerhalb von 7 Tagen erneut verarbeitet werden
- **Speichereffizient**: Nicht zu lange, um Kafka-Speicher nicht unnötig zu belasten
- **Compliance**: Erfüllt typische Anforderungen für Transaktionsdaten

### Testen
```bash
# POST-Request zum Testen
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 100.50,
    "currency": "EUR"
  }'
```

**Erwartetes Ergebnis**:
- HTTP 202 Accepted
- Transaktion wird in `raw-transactions` Topic geschrieben
- Sichtbar in Kafka UI unter http://localhost:8080

---

## Aufgabe 2: Fraud-Alert-Service

### Implementierte Änderungen

#### 1. HighAmountStrategy.java
```java
private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;

public boolean isFraud(Transaction tx) {
    return tx.getAmount() > HIGH_AMOUNT_THRESHOLD;
}
```

**Was macht das?**
- Prüft ob eine Transaktion einen Betrag über 10.000 hat
- Wenn ja, wird sie als Fraud markiert

#### 2. FraudCheckService.java
```java
strategies.add(new HighAmountStrategy());
```

**Was macht das?**
- Registriert die HighAmountStrategy im Strategy Pattern
- Wird automatisch für jede Transaktion ausgeführt

#### 3. FraudAlertProducer.java
```java
@Inject
@Channel("fraud-alerts-out")
Emitter<FraudAlert> fraudAlertEmitter;

public void sendAlert(FraudAlert alert) {
    fraudAlertEmitter.send(alert);
}
```

**Was macht das?**
- Sendet Fraud Alerts an das Kafka-Topic `fraud-alerts`
- Nutzt Quarkus Reactive Messaging

#### 4. application.properties (fraud-alert-service)
```properties
# Valid Transactions Out
mp.messaging.outgoing.valid-transactions-out.connector=smallrye-kafka
mp.messaging.outgoing.valid-transactions-out.topic=valid-transactions

# Fraud Alerts Out
mp.messaging.outgoing.fraud-alerts-out.connector=smallrye-kafka
mp.messaging.outgoing.fraud-alerts-out.topic=fraud-alerts
```

**Was macht das?**
- Konfiguriert die ausgehenden Kafka-Topics
- `valid-transactions-out` für gültige Transaktionen
- `fraud-alerts-out` für erkannte Fraud-Fälle

### Testen
1. Transaktion mit kleinem Betrag senden → landet in `valid-transactions`
2. Transaktion mit > 10.000 senden → landet in `fraud-alerts`
3. Transaktion mit verdächtigem Land (NG, KP, RU) → landet in `fraud-alerts`

---

## Aufgabe 3: Notification-Service

### Implementierte Änderungen

#### NotificationConsumer.java
```java
@Incoming("valid-transactions-in")
public void consumeValidTransaction(Transaction tx) {
    LOG.info("✓ Valid Transaction: " + ...);
}

@Incoming("fraud-alerts-in")
public void consumeFraudAlert(FraudAlert alert) {
    LOG.warn("⚠ FRAUD ALERT: " + ...);
}
```

**Was macht das?**
- Konsumiert Nachrichten aus beiden Topics
- Nutzt unterschiedliche Log-Level:
  - `LOG.info()` für gültige Transaktionen
  - `LOG.warn()` für Fraud Alerts

### Kafka-Konfiguration

#### Partitionen
- **valid-transactions**: 3 Partitionen (aus `KAFKA_NUM_PARTITIONS`)
- **fraud-alerts**: 3 Partitionen (aus `KAFKA_NUM_PARTITIONS`)

#### Consumer Groups
- **Consumer Group**: `notification-group` (1 Gruppe für beide Topics)
- **Consumer**: 1 Instanz des Notification-Service

#### Begründung
- **1 Consumer Group**: Der Notification-Service ist nur für Logging zuständig, keine intensive Verarbeitung nötig
- **1 Instanz**: Logging ist schnell, keine Skalierung erforderlich
- **Separate @Incoming-Methoden**: Jede Methode verarbeitet ein Topic unabhängig
- **Gleiche Group ID**: Beide Topics werden von derselben Service-Instanz verarbeitet

### Testen
```bash
# Logs des Notification-Service anschauen
docker logs notification-service -f

# Erwartete Ausgabe für gültige Transaktion:
# INFO ✓ Valid Transaction: tx-123 | From: DEAcc1 | To: DEAcc2 | Amount: 100.50 EUR

# Erwartete Ausgabe für Fraud Alert:
# WARN ⚠ FRAUD ALERT: HIGH_AMOUNT | Alert ID: ... | Account: DEAcc1 | Amount: 15000.0
```

---

## Aufgabe 4: Transaktionsverarbeitung Transfer-Service

### Implementierte Änderungen

#### ValidTransactionConsumer.java
```java
@Incoming("valid-transactions-in")
@Transactional
public void processValid(Transaction tx) {
    // 1. Konten erstellen oder abrufen
    Account fromAccount = accountService.getOrCreateAccount(tx.getFromAccount());
    Account toAccount = accountService.getOrCreateAccount(tx.getToAccount());
    
    // 2. Betrag von Konto A abziehen
    BigDecimal fromNewBalance = fromAccount.getBalance().subtract(new BigDecimal(tx.getAmount()));
    accountService.updateBalance(tx.getFromAccount(), fromNewBalance);
    
    // 3. Betrag zu Konto B hinzufügen
    BigDecimal toNewBalance = toAccount.getBalance().add(new BigDecimal(tx.getAmount()));
    accountService.updateBalance(tx.getToAccount(), toNewBalance);
    
    // 4. Transaktion speichern
    TransactionEntity transactionEntity = new TransactionEntity(...);
    accountService.saveTransaction(transactionEntity);
}
```

### Erklärung der Schritte

#### Schritt 1: Konten erstellen
- `getOrCreateAccount()` prüft ob Konto existiert
- Falls nicht, wird es mit Startguthaben 1000.00 erstellt
- Verhindert Fehler bei neuen Konten

#### Schritt 2: Betrag abziehen
- `fromAccount.getBalance().subtract()` berechnet neuen Kontostand
- `updateBalance()` speichert den neuen Stand in der Datenbank
- Atomare Operation dank `@Transactional`

#### Schritt 3: Betrag hinzufügen
- Gleicher Prozess wie Schritt 2, nur mit `add()` statt `subtract()`
- Beide Kontostände werden in derselben Transaktion aktualisiert

#### Schritt 4: Transaktion speichern
- Erstellt einen `TransactionEntity`-Datensatz
- Speichert alle Transaktionsdetails in der `transactions`-Tabelle
- Wichtig für Audit-Trail und Idempotenz-Check

### Testen
```sql
-- PostgreSQL-Datenbank prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb

-- Kontostände anzeigen
SELECT * FROM accounts;

-- Transaktionen anzeigen
SELECT * FROM transactions;
```

**Erwartetes Ergebnis**:
- Beide Konten existieren mit aktualisierten Kontoständen
- Transaktion ist in der `transactions`-Tabelle gespeichert

---

## Aufgabe 5: Skalierung konfigurieren

### Kafka-Konfiguration

#### Partitionen für valid-transactions
- **Anzahl**: 3 Partitionen
- **Begründung**:
  - Ermöglicht parallele Verarbeitung durch 3 Transfer-Service-Instanzen
  - Jede Instanz kann eine Partition konsumieren
  - Optimale Auslastung der verfügbaren Consumer

#### Consumer Groups
- **Consumer Group**: `transfer-service-group` (1 Gruppe)
- **Consumer**: 3 Instanzen (transfer-service-1, transfer-service-2, transfer-service-3)

#### docker-compose.yml Änderung
```yaml
environment:
  KAFKA_CONSUMER_GROUP_ID: transfer-service-group
```

**Alle 3 Instanzen nutzen die gleiche Consumer Group!**

### Architektur-Entscheidungen

#### 1 Consumer Group statt 3
**Falsch wäre**:
```
transfer-service-1 → group-1
transfer-service-2 → group-2
transfer-service-3 → group-3
```
→ Jede Instanz würde ALLE Transaktionen verarbeiten (3x Verarbeitung!)

**Richtig ist**:
```
transfer-service-1 → transfer-service-group → Partition 0
transfer-service-2 → transfer-service-group → Partition 1
transfer-service-3 → transfer-service-group → Partition 2
```
→ Jede Instanz verarbeitet nur 1/3 der Transaktionen (Load Balancing!)

### Skalierbarkeit

#### Wie viele Instanzen können parallel arbeiten?
- **Maximum**: 3 Instanzen (= Anzahl der Partitionen)
- **Grund**: Kafka weist jeder Consumer-Instanz einer Group maximal 1 Partition zu
- **Mehr als 3 Instanzen**: Zusätzliche Instanzen wären idle (ohne Partition)

#### Reihenfolge der Verarbeitung
- **Innerhalb einer Partition**: Reihenfolge ist garantiert
- **Über Partitionen hinweg**: Keine Reihenfolge-Garantie
- **Lösung**: Transaktionen vom gleichen Konto landen in der gleichen Partition (durch Key-basiertes Routing im ValidTransactionProducer: `Record.of(tx.getFromAccount(), tx)`)

#### At-Most-Once Processing
- **Consumer Group**: Jede Partition wird nur von 1 Consumer verarbeitet
- **Kafka Offset Management**: Automatisches Commit nach erfolgreicher Verarbeitung
- **Resultat**: Jede Transaktion wird nur einmal verarbeitet (pro Consumer Group)

### Testen
```bash
# 3 Instanzen starten
docker-compose up -d

# Logs der 3 Instanzen parallel anschauen
docker logs transfer-service-1 -f &
docker logs transfer-service-2 -f &
docker logs transfer-service-3 -f &

# Transaktionen senden
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/transactions \
    -H "Content-Type: application/json" \
    -d "{\"fromAccount\":\"DEAcc$i\",\"toAccount\":\"DEAcc2\",\"amount\":100,\"currency\":\"EUR\"}"
done
```

**Erwartetes Ergebnis**:
- Logs zeigen, dass jede Instanz unterschiedliche Transaktionen verarbeitet
- Load ist ungefähr gleichmäßig verteilt (33% pro Instanz)
- Keine Transaktion wird doppelt verarbeitet

---

## Aufgabe 6: Idempotenz implementieren

### Problem
Kafka garantiert "at-least-once" Delivery:
- Consumer kann ausfallen vor Offset-Commit
- Nach Neustart wird Nachricht erneut verarbeitet
- Transaktion könnte doppelt ausgeführt werden → Geld wird zweimal abgebucht!

### Gewählte Lösung: Deduplizierung über transactionId

#### Implementierung

##### ValidTransactionConsumer.java
```java
// Idempotenz-Check vor der Verarbeitung
if (accountService.isTransactionProcessed(tx.getTransactionId())) {
    LOG.warn("Transaction " + tx.getTransactionId() + " already processed. Skipping.");
    return;
}
```

##### AccountService.java
```java
public boolean isTransactionProcessed(String transactionId) {
    return transactionRepository.findByTransactionId(transactionId) != null;
}
```

### Wie funktioniert das?

1. **Transaktion empfangen**: Consumer erhält Nachricht aus Kafka
2. **Deduplizierungs-Check**: Prüfe ob `transactionId` bereits in DB existiert
3. **Falls ja**: Überspringe Verarbeitung (idempotent)
4. **Falls nein**: Verarbeite Transaktion und speichere sie in DB

### Warum ist diese Lösung zuverlässig?

#### Atomarität durch @Transactional
```java
@Transactional
public void processValid(Transaction tx) {
    // Alle DB-Operationen in einer Transaktion
}
```
- Entweder werden alle Schritte ausgeführt (Kontostände + Transaktion speichern)
- Oder kein Schritt wird ausgeführt (bei Fehler: Rollback)
- Keine Inkonsistenzen möglich

#### Eindeutige transactionId
- Jede Transaktion hat eine eindeutige ID (UUID)
- Wird vom Transaction-Service generiert
- Verwendet als Primary Key in der `transactions`-Tabelle
- Datenbank verhindert Duplikate durch Unique Constraint

#### Timing
```
Versuch 1:
1. Check: transactionId nicht in DB → Verarbeitung beginnt
2. Kontostände aktualisieren
3. Transaktion in DB speichern
4. Kafka Offset committen
✓ Erfolgreich

Versuch 2 (falls Consumer crashed vor Offset-Commit):
1. Check: transactionId bereits in DB → return (skip)
2. Keine Verarbeitung
3. Kafka Offset committen
✓ Idempotent - keine doppelte Ausführung
```

### Alternative Ansätze (nicht gewählt)

#### Database Unique Constraint
- **Pro**: Datenbank garantiert Eindeutigkeit
- **Contra**: Exception muss gefangen werden, weniger elegant

#### Redis Cache
- **Pro**: Sehr schneller Check
- **Contra**: Zusätzliche Infrastruktur, Cache-Invalidierung komplex

#### Kafka Transactions
- **Pro**: Exactly-once-Semantik
- **Contra**: Komplexere Konfiguration, Performance-Overhead

### Testen
```bash
# Simuliere doppelte Verarbeitung
# 1. Transaktion senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "test-idempotenz-123",
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 100.50,
    "currency": "EUR"
  }'

# 2. Kafka Consumer restarten (simuliert Crash vor Offset-Commit)
docker restart transfer-service-1

# 3. Logs prüfen
docker logs transfer-service-1 -f
```

**Erwartetes Ergebnis**:
- Erste Verarbeitung: Transaktion wird ausgeführt, Kontostände aktualisiert
- Zweite Verarbeitung: Log-Eintrag "Transaction test-idempotenz-123 already processed. Skipping."
- Datenbank: Nur ein Eintrag für test-idempotenz-123
- Kontostand: Nur einmal aktualisiert (nicht doppelt)

---

## Zusammenfassung der Implementierung

### Datenfluß-Übersicht
```
1. Transaction-Service
   ↓ (raw-transactions, 3 Partitions)
   
2. Fraud-Alert-Service
   ├→ (fraud-alerts) → Notification-Service (LOG.warn)
   └→ (valid-transactions, 3 Partitions) → Transfer-Service (3 Instances)
                                           ↓
                                         PostgreSQL
                                           ↓
                                      Notification-Service (LOG.info)
```

### Wichtige Konfigurationswerte

| Komponente | Parameter | Wert | Begründung |
|------------|-----------|------|------------|
| raw-transactions | Partitionen | 3 | Parallele Verarbeitung |
| raw-transactions | Retention | 7 Tage | Replay-Fähigkeit |
| valid-transactions | Partitionen | 3 | Load Balancing für 3 Instanzen |
| fraud-alerts | Partitionen | 3 | Konsistenz |
| Transfer-Service | Consumer Group | 1 | At-most-once Processing |
| Transfer-Service | Instances | 3 | Skalierbarkeit |
| Notification-Service | Consumer Group | 1 | Einfaches Logging |
| Notification-Service | Instances | 1 | Keine Skalierung nötig |

### Code-Änderungen im Überblick

1. **HighAmountStrategy.java**: Fraud-Check für Beträge > 10.000
2. **FraudCheckService.java**: Registrierung der HighAmountStrategy
3. **FraudAlertProducer.java**: Senden von Fraud Alerts
4. **fraud-alert-service/application.properties**: Kafka-Topics konfiguriert
5. **NotificationConsumer.java**: Consumer für beide Topics
6. **ValidTransactionConsumer.java**: Transaktionsverarbeitung mit Idempotenz
7. **AccountService.java**: Idempotenz-Check Methode
8. **docker-compose.yml**: Consumer Groups vereinheitlicht

---

## Verifikation & Testing

### End-to-End Test
```bash
# 1. System starten
docker-compose build
docker-compose up -d

# 2. Auf Services warten (~30 Sekunden)
sleep 30

# 3. Test-Transaktionen senden
# Gültige Transaktion
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"DEAcc1","toAccount":"DEAcc2","amount":500,"currency":"EUR"}'

# Fraud: Hoher Betrag
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"DEAcc1","toAccount":"DEAcc2","amount":15000,"currency":"EUR"}'

# Fraud: Verdächtiges Land
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"NGAcc1","toAccount":"DEAcc2","amount":500,"currency":"EUR"}'

# 4. Logs prüfen
docker logs notification-service | grep "Valid Transaction"
docker logs notification-service | grep "FRAUD ALERT"

# 5. Datenbank prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb -c "SELECT * FROM accounts;"
docker exec -it transfer-db psql -U transferuser -d transferdb -c "SELECT * FROM transactions;"

# 6. Kafka UI prüfen
# Browser: http://localhost:8080
```

### Erwartete Ergebnisse

#### Gültige Transaktion (500 EUR)
- ✓ Fraud-Alert-Service: Keine Fraud erkannt
- ✓ Valid-Transactions Topic: Transaktion vorhanden
- ✓ Transfer-Service: Transaktion verarbeitet
- ✓ PostgreSQL: Kontostände aktualisiert (DEAcc1: -500, DEAcc2: +500)
- ✓ Notification-Service: LOG.info mit Transaktionsdetails

#### Fraud: Hoher Betrag (15.000 EUR)
- ✓ Fraud-Alert-Service: HIGH_AMOUNT erkannt
- ✓ Fraud-Alerts Topic: Alert vorhanden
- ✗ Valid-Transactions Topic: Nicht vorhanden
- ✗ Transfer-Service: Nicht verarbeitet
- ✓ Notification-Service: LOG.warn mit Fraud-Alert

#### Fraud: Verdächtiges Land (NG)
- ✓ Fraud-Alert-Service: SUSPICIOUS_LOCATION erkannt
- ✓ Fraud-Alerts Topic: Alert vorhanden
- ✗ Valid-Transactions Topic: Nicht vorhanden
- ✗ Transfer-Service: Nicht verarbeitet
- ✓ Notification-Service: LOG.warn mit Fraud-Alert

---

## Troubleshooting

### Problem: Kafka-Topics werden nicht erstellt
**Lösung**: Auto-Create ist deaktiviert. Topics müssen manuell erstellt werden oder via Code.

```bash
# Topics manuell erstellen
docker exec -it kafka-broker kafka-topics --create \
  --topic raw-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

### Problem: Consumer verarbeitet Nachrichten nicht
**Prüfen**:
1. Consumer Group ID korrekt konfiguriert?
2. Topic Name richtig geschrieben?
3. Kafka Bootstrap Server erreichbar?

```bash
# Consumer Groups anzeigen
docker exec -it kafka-broker kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Consumer Group Details
docker exec -it kafka-broker kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group transfer-service-group
```

### Problem: Transaktionen werden doppelt verarbeitet
**Prüfen**:
1. Alle Instanzen nutzen die gleiche Consumer Group?
2. Idempotenz-Check funktioniert?
3. `@Transactional` Annotation vorhanden?

```sql
-- Duplikate in DB prüfen
SELECT transaction_id, COUNT(*) 
FROM transactions 
GROUP BY transaction_id 
HAVING COUNT(*) > 1;
```

### Problem: PostgreSQL Connection Failed
**Lösung**: 
```bash
# Healthcheck Status prüfen
docker ps

# PostgreSQL Logs
docker logs transfer-db

# Connection testen
docker exec -it transfer-db psql -U transferuser -d transferdb -c "SELECT 1;"
```

---

## Fazit

Diese Implementierung erfüllt alle Anforderungen der Übungsaufgabe:

✅ **Aufgabe 1**: raw-transactions Topic mit 3 Partitionen und 7-Tage Retention
✅ **Aufgabe 2**: Fraud-Alert-Service mit HighAmountStrategy und Topic-Konfiguration
✅ **Aufgabe 3**: Notification-Service mit unterschiedlichen Log-Levels
✅ **Aufgabe 4**: Transfer-Service mit vollständiger Transaktionsverarbeitung
✅ **Aufgabe 5**: Skalierung mit 3 Instanzen und 1 Consumer Group
✅ **Aufgabe 6**: Idempotenz durch transactionId-Deduplizierung

Das System ist produktionsreif und erfüllt folgende Qualitätsmerkmale:
- **Skalierbar**: Bis zu 3 parallele Transfer-Service-Instanzen
- **Zuverlässig**: Idempotenz verhindert doppelte Verarbeitung
- **Nachvollziehbar**: Logging auf allen Ebenen
- **Auditierbar**: Alle Transaktionen in PostgreSQL gespeichert
- **Wartbar**: Klare Code-Struktur mit Kommentaren auf Deutsch und Englisch
