# Quick Reference - Was wurde wo implementiert?

## 📁 Datei-Übersicht der Änderungen

### 1. Fraud-Alert-Service

#### `fraud-alert-service/src/main/java/de/berlin/htw/control/HighAmountStrategy.java`
```java
// ÄNDERUNG: Implementierung der Fraud-Erkennung für hohe Beträge
private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;

public boolean isFraud(Transaction tx) {
    return tx.getAmount() > HIGH_AMOUNT_THRESHOLD;
}
```
**Was macht das?**: Prüft ob Transaktion über 10.000 EUR liegt

#### `fraud-alert-service/src/main/java/de/berlin/htw/control/FraudCheckService.java`
```java
// ÄNDERUNG: Registrierung der HighAmountStrategy
strategies.add(new HighAmountStrategy());
```
**Was macht das?**: Fügt den High-Amount-Check zur Prüfliste hinzu

#### `fraud-alert-service/src/main/java/de/berlin/htw/boundary/FraudAlertProducer.java`
```java
// ÄNDERUNG: Kafka Producer hinzugefügt
@Inject
@Channel("fraud-alerts-out")
Emitter<FraudAlert> fraudAlertEmitter;

public void sendAlert(FraudAlert alert) {
    fraudAlertEmitter.send(alert);
}
```
**Was macht das?**: Sendet Fraud Alerts an Kafka Topic `fraud-alerts`

#### `fraud-alert-service/src/main/resources/application.properties`
```properties
# ÄNDERUNG: Ausgehende Topics konfiguriert
mp.messaging.outgoing.valid-transactions-out.connector=smallrye-kafka
mp.messaging.outgoing.valid-transactions-out.topic=valid-transactions

mp.messaging.outgoing.fraud-alerts-out.connector=smallrye-kafka
mp.messaging.outgoing.fraud-alerts-out.topic=fraud-alerts
```
**Was macht das?**: Verbindet die Producer mit den Kafka Topics

---

### 2. Notification-Service

#### `notification-service/src/main/java/de/berlin/htw/boundary/NotificationConsumer.java`
```java
// ÄNDERUNG: Consumer für valid-transactions hinzugefügt
@Incoming("valid-transactions-in")
public void consumeValidTransaction(Transaction tx) {
    LOG.info("✓ Valid Transaction: " + ...);
}

// ÄNDERUNG: Consumer für fraud-alerts hinzugefügt
@Incoming("fraud-alerts-in")
public void consumeFraudAlert(FraudAlert alert) {
    LOG.warn("⚠ FRAUD ALERT: " + ...);
}
```
**Was macht das?**: 
- Liest aus beiden Topics
- Loggt mit unterschiedlichen Log-Levels (INFO vs WARN)

---

### 3. Transfer-Service

#### `transfer-service/src/main/java/de/berlin/htw/boundary/ValidTransactionConsumer.java`
```java
// ÄNDERUNG: Imports hinzugefügt
import de.berlin.htw.entity.Account;
import de.berlin.htw.entity.TransactionEntity;
import java.math.BigDecimal;

// ÄNDERUNG: Idempotenz-Check hinzugefügt
if (accountService.isTransactionProcessed(tx.getTransactionId())) {
    LOG.warn("Transaction already processed. Skipping.");
    return;
}

// ÄNDERUNG: Transaktionsverarbeitung implementiert
Account fromAccount = accountService.getOrCreateAccount(tx.getFromAccount());
Account toAccount = accountService.getOrCreateAccount(tx.getToAccount());

BigDecimal fromNewBalance = fromAccount.getBalance().subtract(new BigDecimal(tx.getAmount()));
accountService.updateBalance(tx.getFromAccount(), fromNewBalance);

BigDecimal toNewBalance = toAccount.getBalance().add(new BigDecimal(tx.getAmount()));
accountService.updateBalance(tx.getToAccount(), toNewBalance);

TransactionEntity transactionEntity = new TransactionEntity(...);
accountService.saveTransaction(transactionEntity);
```
**Was macht das?**:
1. Prüft ob Transaktion bereits verarbeitet (Idempotenz)
2. Erstellt Konten falls nötig
3. Zieht Betrag von Konto A ab
4. Fügt Betrag zu Konto B hinzu
5. Speichert Transaktion in DB

#### `transfer-service/src/main/java/de/berlin/htw/control/AccountService.java`
```java
// ÄNDERUNG: Idempotenz-Check Methode hinzugefügt
public boolean isTransactionProcessed(String transactionId) {
    return transactionRepository.findByTransactionId(transactionId) != null;
}
```
**Was macht das?**: Prüft ob transactionId bereits in Datenbank existiert

---

### 4. Docker Compose

#### `docker-compose.yml`
```yaml
# ÄNDERUNG: Alle 3 Transfer-Service Instanzen nutzen die gleiche Consumer Group
transfer-service-1:
  environment:
    KAFKA_CONSUMER_GROUP_ID: transfer-service-group  # ← GEÄNDERT

transfer-service-2:
  environment:
    KAFKA_CONSUMER_GROUP_ID: transfer-service-group  # ← GEÄNDERT

transfer-service-3:
  environment:
    KAFKA_CONSUMER_GROUP_ID: transfer-service-group  # ← GEÄNDERT
```
**Was macht das?**: Ermöglicht Load Balancing zwischen den 3 Instanzen

---

## 🔍 Wo prüfen ob es funktioniert?

### Fraud-Alert-Service testen
```bash
# 1. Gültige Transaktion senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"DEAcc1","toAccount":"DEAcc2","amount":500,"currency":"EUR"}'

# 2. Logs prüfen
docker logs fraud-alert-service | grep "Valid Transaction"

# Erwartung: "Valid Transaction sent: ..."
```

```bash
# 1. Fraud: Hoher Betrag
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"DEAcc1","toAccount":"DEAcc2","amount":15000,"currency":"EUR"}'

# 2. Logs prüfen
docker logs fraud-alert-service | grep "Fraud Alert"

# Erwartung: "Fraud Alert sent: HIGH_AMOUNT for account DEAcc1"
```

### Notification-Service testen
```bash
# Logs anschauen
docker logs notification-service

# Erwartung für gültige Transaktion:
# INFO  ✓ Valid Transaction: tx-... | From: DEAcc1 | To: DEAcc2 | Amount: 500.0 EUR

# Erwartung für Fraud Alert:
# WARN  ⚠ FRAUD ALERT: HIGH_AMOUNT | Alert ID: ... | Account: DEAcc1 | Amount: 15000.0
```

### Transfer-Service testen
```bash
# 1. Transaktion senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"TestAcc1","toAccount":"TestAcc2","amount":200,"currency":"EUR"}'

# 2. Logs prüfen (eine der 3 Instanzen)
docker logs transfer-service-1 | grep "TestAcc"
docker logs transfer-service-2 | grep "TestAcc"
docker logs transfer-service-3 | grep "TestAcc"

# Erwartung:
# INFO  Received transaction: ... from TestAcc1 to TestAcc2 amount: 200.0
# INFO  Updated balance for TestAcc1: 800.00
# INFO  Updated balance for TestAcc2: 1200.00
# INFO  Transaction saved: ...

# 3. Datenbank prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT account_id, balance FROM accounts WHERE account_id IN ('TestAcc1', 'TestAcc2');"

# Erwartung:
#  account_id | balance
# ------------+---------
#  TestAcc1   | 800.00
#  TestAcc2   | 1200.00
```

### Idempotenz testen
```bash
# 1. Transaktion mit fester ID senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"test-123","fromAccount":"IdempAcc1","toAccount":"IdempAcc2","amount":300,"currency":"EUR"}'

sleep 5

# 2. GLEICHE Transaktion nochmal senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"test-123","fromAccount":"IdempAcc1","toAccount":"IdempAcc2","amount":300,"currency":"EUR"}'

sleep 5

# 3. Logs prüfen
docker logs transfer-service-1 | grep "test-123"

# Erwartung:
# INFO  Received transaction: test-123 ...
# INFO  Updated balance for IdempAcc1: 700.00
# INFO  Updated balance for IdempAcc2: 1300.00
# INFO  Transaction saved: test-123
# INFO  Received transaction: test-123 ...  ← ZWEITE VERARBEITUNG
# WARN  Transaction test-123 already processed. Skipping.  ← IDEMPOTENZ!

# 4. Datenbank prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT COUNT(*) FROM transactions WHERE transaction_id = 'test-123';"

# Erwartung: count = 1 (nur einmal in DB!)
```

### Skalierung/Load Balancing testen
```bash
# 1. Mehrere Transaktionen senden
for i in {1..9}; do
  curl -X POST http://localhost:8081/api/transactions \
    -H "Content-Type: application/json" \
    -d "{\"fromAccount\":\"ScaleAcc${i}\",\"toAccount\":\"TargetAcc\",\"amount\":100,\"currency\":\"EUR\"}"
  sleep 1
done

# 2. Logs aller 3 Instanzen prüfen
echo "=== Instance 1 ==="
docker logs transfer-service-1 | grep "ScaleAcc" | wc -l

echo "=== Instance 2 ==="
docker logs transfer-service-2 | grep "ScaleAcc" | wc -l

echo "=== Instance 3 ==="
docker logs transfer-service-3 | grep "ScaleAcc" | wc -l

# Erwartung: Alle 3 Instanzen haben Transaktionen verarbeitet (ca. 3 pro Instanz)
```

---

## 🎯 Schnellcheck: Alles funktioniert?

### ✅ Checklist
```bash
# System starten
docker-compose build && docker-compose up -d
sleep 60  # Warten bis alles läuft

# Test 1: Gültige Transaktion
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"Check1","toAccount":"Check2","amount":100,"currency":"EUR"}'

# ✅ Erwartung: 
# - Fraud-Alert-Service: "Valid Transaction sent"
# - Notification-Service: INFO log mit "Valid Transaction"
# - Transfer-Service: Kontostände aktualisiert
# - PostgreSQL: Transaktion in DB

# Test 2: Fraud (Hoher Betrag)
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"Check3","toAccount":"Check4","amount":20000,"currency":"EUR"}'

# ✅ Erwartung:
# - Fraud-Alert-Service: "Fraud Alert sent: HIGH_AMOUNT"
# - Notification-Service: WARN log mit "FRAUD ALERT"
# - Transfer-Service: KEINE Verarbeitung
# - PostgreSQL: KEINE Transaktion in DB

# Test 3: Idempotenz
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"idem-123","fromAccount":"Check5","toAccount":"Check6","amount":150,"currency":"EUR"}'
sleep 5
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"idem-123","fromAccount":"Check5","toAccount":"Check6","amount":150,"currency":"EUR"}'

# ✅ Erwartung:
# - Transfer-Service: "Transaction idem-123 already processed. Skipping."
# - PostgreSQL: Nur 1 Eintrag für idem-123
```

---

## 📊 Kafka UI Überprüfung

**URL**: http://localhost:8080

### Topics prüfen
1. **raw-transactions**
   - ✅ Existiert
   - ✅ 3 Partitionen
   - ✅ Enthält alle gesendeten Transaktionen

2. **valid-transactions**
   - ✅ Existiert
   - ✅ 3 Partitionen
   - ✅ Enthält nur gültige Transaktionen (keine Fraud)

3. **fraud-alerts**
   - ✅ Existiert
   - ✅ 3 Partitionen
   - ✅ Enthält nur Fraud Alerts

### Consumer Groups prüfen
1. **fraud-check-group**
   - ✅ 1 Member (Fraud-Alert-Service)
   - ✅ Konsumiert raw-transactions
   - ✅ LAG = 0

2. **notification-group**
   - ✅ 1 Member (Notification-Service)
   - ✅ Konsumiert valid-transactions UND fraud-alerts
   - ✅ LAG = 0

3. **transfer-service-group**
   - ✅ 3 Members (3 Transfer-Service Instanzen)
   - ✅ Konsumiert valid-transactions
   - ✅ Jeder Member hat 1 Partition zugewiesen
   - ✅ LAG = 0

---

## 🐛 Troubleshooting

### Problem: Topic existiert nicht
```bash
# Manuell erstellen
docker exec -it kafka-broker /opt/kafka/bin/kafka-topics.sh \
  --create --topic raw-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1
```

### Problem: Consumer hängen (LAG > 0)
```bash
# Consumer Group Status prüfen
docker exec -it kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group transfer-service-group
```

### Problem: Service startet nicht
```bash
# Logs anschauen
docker logs <service-name>

# Häufige Probleme:
# - Kafka not ready → länger warten (60s)
# - PostgreSQL not ready → länger warten (30s)
# - Port belegt → docker-compose down, dann up
```

### Problem: Transaktion landet nicht in DB
```bash
# 1. Ist sie als Fraud erkannt worden?
docker logs fraud-alert-service | grep "Fraud Alert"

# 2. Welche Instanz verarbeitet?
docker logs transfer-service-1 | tail -20
docker logs transfer-service-2 | tail -20
docker logs transfer-service-3 | tail -20
```

---

## 📚 Dokumentation

- **IMPLEMENTATION_DOCUMENTATION.md**: Vollständige technische Dokumentation
- **TESTING_GUIDE.md**: Ausführliche Testanleitung
- **SUMMARY_GERMAN.md**: Zusammenfassung aller Änderungen
- **QUICK_REFERENCE.md**: Diese Datei

---

## ⚡ Schnellstart

```bash
# 1. Bauen
mvn clean package -DskipTests
docker-compose build

# 2. Starten
docker-compose up -d

# 3. Warten
sleep 60

# 4. Testen
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"QuickTest1","toAccount":"QuickTest2","amount":250,"currency":"EUR"}'

# 5. Überprüfen
docker logs notification-service | grep "QuickTest"
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT * FROM transactions WHERE from_account = 'QuickTest1';"

# 6. Kafka UI öffnen
# Browser: http://localhost:8080
```

---

**Fertig! Alle 6 Aufgaben sind implementiert und getestet! ✅**
