# Test- und Verifikationsanleitung

## Schritt-für-Schritt Anleitung zum Testen aller Aufgaben

Diese Anleitung zeigt dir genau, wie du alle implementierten Features testen und überprüfen kannst.

---

## Voraussetzungen

Stelle sicher, dass folgende Software installiert ist:
- Docker & Docker Compose
- curl (für HTTP-Requests)
- Optional: PostgreSQL Client (psql) oder DBeaver für Datenbankabfragen

---

## 1. System Starten

### Projekt bauen
```bash
cd /home/runner/work/verteilte-anwendungen-lab4-main/verteilte-anwendungen-lab4-main

# Maven Build
mvn clean package -DskipTests

# Docker Images bauen
docker-compose build
```

**Erwartete Ausgabe**: 
- Maven Build erfolgreich
- Alle Docker Images werden gebaut (transaction-service, fraud-alert-service, notification-service, transfer-service)

### System starten
```bash
docker-compose up -d
```

**Erwartete Ausgabe**:
```
Creating network "verteilte-anwendungen-lab4-main_banking-network" with driver "bridge"
Creating kafka-broker ... done
Creating transfer-db ... done
Creating transaction-service ... done
Creating fraud-alert-service ... done
Creating notification-service ... done
Creating transfer-service-1 ... done
Creating transfer-service-2 ... done
Creating transfer-service-3 ... done
```

### Warten bis alles läuft
```bash
# 30-60 Sekunden warten, bis alle Services hochgefahren sind
sleep 60

# Status prüfen
docker-compose ps
```

**Erwartete Ausgabe**: Alle Services sollten Status "Up" haben

---

## 2. Aufgabe 1 testen: raw-transactions Topic

### Topic-Konfiguration überprüfen

```bash
# Kafka UI öffnen im Browser
# URL: http://localhost:8080

# Oder via Command Line:
docker exec -it kafka-broker kafka-topics --bootstrap-server localhost:9092 --list
```

**Erwartete Ausgabe**:
- Topic `raw-transactions` existiert
- Im Kafka UI: Topic hat 3 Partitionen

### Transaktion über REST API senden

```bash
# Gültige Transaktion senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 100.50,
    "currency": "EUR"
  }'
```

**Erwartete Ausgabe**:
```json
{
  "transactionId": "...",
  "fromAccount": "DEAcc1",
  "toAccount": "DEAcc2",
  "amount": 100.50,
  "currency": "EUR",
  "timestamp": "..."
}
```
HTTP Status: 202 Accepted

### Nachricht im Topic prüfen

Im Kafka UI (http://localhost:8080):
1. Navigiere zu Topics → raw-transactions
2. Klicke auf "Messages"
3. Du solltest die gesendete Transaktion sehen

**Oder via Command Line**:
```bash
docker exec -it kafka-broker kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic raw-transactions \
  --from-beginning \
  --max-messages 5
```

---

## 3. Aufgabe 2 testen: Fraud-Alert-Service

### Test 1: Gültige Transaktion (sollte NICHT als Fraud erkannt werden)

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 500.00,
    "currency": "EUR"
  }'
```

**Was passiert**:
1. Transaktion landet in `raw-transactions`
2. Fraud-Alert-Service liest Transaktion
3. HighAmountStrategy prüft: 500 < 10000 → OK
4. LocationStrategy prüft: "DE" ist nicht verdächtig → OK
5. Transaktion landet in `valid-transactions` Topic

**Überprüfen**:
```bash
# Fraud-Alert-Service Logs
docker logs fraud-alert-service | grep "Valid Transaction"

# Oder Kafka UI:
# Topics → valid-transactions → Messages
# Du solltest die Transaktion dort sehen
```

### Test 2: Fraud durch hohen Betrag (> 10.000)

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 15000.00,
    "currency": "EUR"
  }'
```

**Was passiert**:
1. Transaktion landet in `raw-transactions`
2. Fraud-Alert-Service liest Transaktion
3. HighAmountStrategy prüft: 15000 > 10000 → **FRAUD!**
4. Transaktion landet in `fraud-alerts` Topic

**Überprüfen**:
```bash
# Fraud-Alert-Service Logs
docker logs fraud-alert-service | grep "Fraud Alert"

# Oder Kafka UI:
# Topics → fraud-alerts → Messages
# Du solltest den Fraud Alert dort sehen
```

### Test 3: Fraud durch verdächtiges Land (NG, KP, RU)

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "NGAcc1",
    "toAccount": "DEAcc2",
    "amount": 500.00,
    "currency": "EUR"
  }'
```

**Was passiert**:
1. Transaktion landet in `raw-transactions`
2. Fraud-Alert-Service liest Transaktion
3. LocationStrategy prüft: "NG" ist verdächtig → **FRAUD!**
4. Transaktion landet in `fraud-alerts` Topic

**Überprüfen**:
```bash
docker logs fraud-alert-service | grep "SUSPICIOUS_LOCATION"
```

---

## 4. Aufgabe 3 testen: Notification-Service

Der Notification-Service liest automatisch aus beiden Topics und loggt die Nachrichten.

### Gültige Transaktionen (LOG.info)

```bash
# Logs des Notification-Service anschauen
docker logs notification-service | grep "Valid Transaction"
```

**Erwartete Ausgabe**:
```
INFO  ✓ Valid Transaction: tx-... | From: DEAcc1 | To: DEAcc2 | Amount: 500.0 EUR
```

### Fraud Alerts (LOG.warn)

```bash
docker logs notification-service | grep "FRAUD ALERT"
```

**Erwartete Ausgabe**:
```
WARN  ⚠ FRAUD ALERT: HIGH_AMOUNT | Alert ID: ... | Account: DEAcc1 | Amount: 15000.0
WARN  ⚠ FRAUD ALERT: SUSPICIOUS_LOCATION | Alert ID: ... | Account: NGAcc1 | Amount: 500.0
```

### Live-Monitoring

```bash
# Logs in Echtzeit verfolgen
docker logs notification-service -f

# In einem anderen Terminal:
# Neue Transaktionen senden und live sehen wie sie geloggt werden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"DEAcc5","toAccount":"DEAcc6","amount":250,"currency":"EUR"}'
```

---

## 5. Aufgabe 4 testen: Transfer-Service Transaktionsverarbeitung

### Transaktion senden

```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "TestAcc1",
    "toAccount": "TestAcc2",
    "amount": 200.00,
    "currency": "EUR"
  }'
```

### Logs überprüfen

```bash
# Transfer-Service Logs (eine der 3 Instanzen wird verarbeiten)
docker logs transfer-service-1 | grep "TestAcc"
docker logs transfer-service-2 | grep "TestAcc"
docker logs transfer-service-3 | grep "TestAcc"
```

**Erwartete Ausgabe** (in einer der Instanzen):
```
INFO  [transfer-service-group] Received transaction: ... from TestAcc1 to TestAcc2 amount: 200.0
INFO  Updated balance for TestAcc1: 800.00
INFO  Updated balance for TestAcc2: 1200.00
INFO  Transaction saved: ...
```

### Datenbank überprüfen

```bash
# In PostgreSQL einloggen
docker exec -it transfer-db psql -U transferuser -d transferdb
```

**SQL-Abfragen in psql**:

```sql
-- Alle Konten anzeigen
SELECT account_id, balance, created_at, updated_at 
FROM accounts 
ORDER BY account_id;
```

**Erwartete Ausgabe**:
```
 account_id | balance  |         created_at         |         updated_at
------------+----------+----------------------------+----------------------------
 TestAcc1   |  800.00  | 2024-01-15 10:30:00.123456 | 2024-01-15 10:30:00.123456
 TestAcc2   | 1200.00  | 2024-01-15 10:30:00.123456 | 2024-01-15 10:30:00.123456
```

**Erklärung**:
- TestAcc1 wurde mit 1000.00 erstellt, 200.00 abgezogen → 800.00
- TestAcc2 wurde mit 1000.00 erstellt, 200.00 hinzugefügt → 1200.00

```sql
-- Alle Transaktionen anzeigen
SELECT transaction_id, from_account, to_account, amount, currency, processed_at 
FROM transactions 
ORDER BY processed_at DESC 
LIMIT 10;
```

**Erwartete Ausgabe**:
```
 transaction_id | from_account | to_account | amount  | currency |       processed_at
----------------+--------------+------------+---------+----------+----------------------------
 tx-...         | TestAcc1     | TestAcc2   | 200.00  | EUR      | 2024-01-15 10:30:00.123456
```

```sql
-- Beenden
\q
```

---

## 6. Aufgabe 5 testen: Skalierung

### Load Balancing zwischen 3 Instanzen

```bash
# 10 Transaktionen senden
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/transactions \
    -H "Content-Type: application/json" \
    -d "{\"fromAccount\":\"Account${i}\",\"toAccount\":\"TargetAcc\",\"amount\":100,\"currency\":\"EUR\"}"
  echo " - Transaction $i sent"
  sleep 1
done
```

### Logs aller 3 Instanzen parallel anschauen

**Terminal 1**:
```bash
docker logs transfer-service-1 -f | grep "Received transaction"
```

**Terminal 2**:
```bash
docker logs transfer-service-2 -f | grep "Received transaction"
```

**Terminal 3**:
```bash
docker logs transfer-service-3 -f | grep "Received transaction"
```

**Erwartetes Ergebnis**:
- Alle 3 Instanzen verarbeiten Transaktionen
- Load ist ungefähr gleichmäßig verteilt (nicht exakt 33%, aber ähnlich)
- Keine Transaktion wird von mehreren Instanzen verarbeitet

### Consumer Group Statistik

```bash
# Consumer Group Details anzeigen
docker exec -it kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group transfer-service-group
```

**Erwartete Ausgabe**:
```
GROUP                    TOPIC              PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG  CONSUMER-ID
transfer-service-group   valid-transactions 0          15              15              0    consumer-1
transfer-service-group   valid-transactions 1          13              13              0    consumer-2
transfer-service-group   valid-transactions 2          12              12              0    consumer-3
```

**Erklärung**:
- 3 Partitionen (0, 1, 2)
- 3 Consumer (consumer-1, consumer-2, consumer-3)
- Jeder Consumer ist einer Partition zugewiesen
- LAG = 0 bedeutet alle Nachrichten sind verarbeitet

---

## 7. Aufgabe 6 testen: Idempotenz

### Test-Szenario: Doppelte Verarbeitung simulieren

```bash
# 1. Transaktion mit fester ID senden
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "idempotenz-test-123",
    "fromAccount": "IdempAcc1",
    "toAccount": "IdempAcc2",
    "amount": 300.00,
    "currency": "EUR"
  }'

# Warten bis verarbeitet
sleep 5

# 2. Kontostände VORHER notieren
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT account_id, balance FROM accounts WHERE account_id IN ('IdempAcc1', 'IdempAcc2');"
```

**Ausgabe notieren** (z.B. IdempAcc1: 700.00, IdempAcc2: 1300.00)

```bash
# 3. Gleiche Transaktion nochmal senden (simuliert Kafka Retry)
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "idempotenz-test-123",
    "fromAccount": "IdempAcc1",
    "toAccount": "IdempAcc2",
    "amount": 300.00,
    "currency": "EUR"
  }'

# Warten
sleep 5

# 4. Kontostände NACHHER prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT account_id, balance FROM accounts WHERE account_id IN ('IdempAcc1', 'IdempAcc2');"
```

**Erwartetes Ergebnis**: 
- Kontostände sind **unverändert**!
- IdempAcc1: immer noch 700.00
- IdempAcc2: immer noch 1300.00

```bash
# 5. Logs prüfen
docker logs transfer-service-1 | grep "idempotenz-test-123"
docker logs transfer-service-2 | grep "idempotenz-test-123"
docker logs transfer-service-3 | grep "idempotenz-test-123"
```

**Erwartete Log-Ausgabe**:
```
INFO  [transfer-service-group] Received transaction: idempotenz-test-123 from IdempAcc1 to IdempAcc2 amount: 300.0
INFO  Updated balance for IdempAcc1: 700.00
INFO  Updated balance for IdempAcc2: 1300.00
INFO  Transaction saved: idempotenz-test-123

WARN  Transaction idempotenz-test-123 already processed. Skipping.
```

**Erklärung**:
- Erste Verarbeitung: Transaktion wird ausgeführt
- Zweite Verarbeitung: Idempotenz-Check schlägt an → Skip!
- Geld wird nur einmal transferiert, nicht zweimal

### Duplikat-Check in Datenbank

```bash
docker exec -it transfer-db psql -U transferuser -d transferdb
```

```sql
-- Prüfe ob Transaktion nur einmal existiert
SELECT transaction_id, COUNT(*) 
FROM transactions 
WHERE transaction_id = 'idempotenz-test-123'
GROUP BY transaction_id;
```

**Erwartete Ausgabe**:
```
 transaction_id       | count
----------------------+-------
 idempotenz-test-123  |   1
```

**Erklärung**: Transaktion existiert nur einmal in der Datenbank!

---

## 8. End-to-End Test

### Kompletter Durchlauf

```bash
# 1. 5 verschiedene Transaktionen senden

# Gültig
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"E2EAcc1","toAccount":"E2EAcc2","amount":150,"currency":"EUR"}'

# Gültig
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"E2EAcc3","toAccount":"E2EAcc4","amount":500,"currency":"EUR"}'

# Fraud: Hoher Betrag
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"E2EAcc5","toAccount":"E2EAcc6","amount":20000,"currency":"EUR"}'

# Fraud: Verdächtiges Land
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"KPAcc1","toAccount":"E2EAcc7","amount":300,"currency":"EUR"}'

# Gültig
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"E2EAcc8","toAccount":"E2EAcc9","amount":750,"currency":"EUR"}'

sleep 10
```

### Ergebnisse überprüfen

```bash
# Notification-Service Logs
echo "=== VALID TRANSACTIONS ==="
docker logs notification-service | grep "Valid Transaction" | grep "E2E"

echo "=== FRAUD ALERTS ==="
docker logs notification-service | grep "FRAUD ALERT" | grep -E "E2E|KP"
```

**Erwartete Ausgabe**:
```
=== VALID TRANSACTIONS ===
INFO  ✓ Valid Transaction: ... | From: E2EAcc1 | To: E2EAcc2 | Amount: 150.0 EUR
INFO  ✓ Valid Transaction: ... | From: E2EAcc3 | To: E2EAcc4 | Amount: 500.0 EUR
INFO  ✓ Valid Transaction: ... | From: E2EAcc8 | To: E2EAcc9 | Amount: 750.0 EUR

=== FRAUD ALERTS ===
WARN  ⚠ FRAUD ALERT: HIGH_AMOUNT | Alert ID: ... | Account: E2EAcc5 | Amount: 20000.0
WARN  ⚠ FRAUD ALERT: SUSPICIOUS_LOCATION | Alert ID: ... | Account: KPAcc1 | Amount: 300.0
```

```bash
# Datenbank: Nur gültige Transaktionen sollten dort sein
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT COUNT(*) as valid_transactions FROM transactions WHERE from_account LIKE 'E2E%' OR to_account LIKE 'E2E%';"
```

**Erwartete Ausgabe**: `3` (nur die gültigen Transaktionen)

---

## 9. Kafka UI - Visuelle Überprüfung

Öffne im Browser: **http://localhost:8080**

### Topics überprüfen

1. **raw-transactions**
   - Partitions: 3
   - Messages: Alle gesendeten Transaktionen

2. **valid-transactions**
   - Partitions: 3
   - Messages: Nur gültige Transaktionen (ohne Fraud)

3. **fraud-alerts**
   - Partitions: 3
   - Messages: Nur Fraud Alerts

### Consumer Groups überprüfen

1. **fraud-check-group**
   - Members: 1
   - Topics: raw-transactions

2. **notification-group**
   - Members: 1 (konsumiert 2 Topics)
   - Topics: valid-transactions, fraud-alerts

3. **transfer-service-group**
   - Members: 3 (die 3 Instanzen)
   - Topics: valid-transactions
   - Jede Instanz hat eine Partition zugewiesen

---

## 10. Performance & Monitoring

### Kafka Lag überprüfen

```bash
# Prüfe ob Consumer hinterherhängen
docker exec -it kafka-broker kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --all-groups
```

**Erwartung**: LAG sollte bei allen Groups 0 oder sehr niedrig sein

### Service Health

```bash
# Prüfe ob alle Services laufen
docker-compose ps

# CPU & Memory Usage
docker stats --no-stream
```

### Database Größe

```bash
docker exec -it transfer-db psql -U transferuser -d transferdb
```

```sql
-- Anzahl Konten
SELECT COUNT(*) as total_accounts FROM accounts;

-- Anzahl Transaktionen
SELECT COUNT(*) as total_transactions FROM transactions;

-- Durchschnittlicher Kontostand
SELECT AVG(balance) as avg_balance FROM accounts;

-- Größte Transaktion
SELECT * FROM transactions ORDER BY amount DESC LIMIT 1;
```

---

## 11. Troubleshooting

### Problem: Service startet nicht

```bash
# Logs anschauen
docker logs <service-name>

# Beispiel:
docker logs transfer-service-1

# Häufige Probleme:
# - Kafka nicht erreichbar → Warte länger
# - PostgreSQL nicht ready → Warte länger
# - Port bereits belegt → docker-compose down, dann up
```

### Problem: Transaktion landet nicht in DB

```bash
# 1. Prüfe Transaction-Service
docker logs transaction-service | tail -20

# 2. Prüfe Fraud-Alert-Service
docker logs fraud-alert-service | tail -20

# 3. Prüfe Transfer-Service
docker logs transfer-service-1 | tail -20
docker logs transfer-service-2 | tail -20
docker logs transfer-service-3 | tail -20

# 4. Prüfe ob Transaktion als Fraud erkannt wurde
docker logs fraud-alert-service | grep "Fraud Alert"
```

### Problem: Kafka Topics existieren nicht

```bash
# Topics manuell erstellen
docker exec -it kafka-broker kafka-topics \
  --create \
  --topic raw-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it kafka-broker kafka-topics \
  --create \
  --topic valid-transactions \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec -it kafka-broker kafka-topics \
  --create \
  --topic fraud-alerts \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

---

## 12. System Herunterfahren

```bash
# Alles stoppen und Container entfernen
docker-compose down

# Zusätzlich Volumes löschen (Datenbank wird zurückgesetzt)
docker-compose down -v
```

---

## Zusammenfassung der Testkriterien

### ✅ Aufgabe 1: raw-transactions Topic
- [ ] Topic existiert mit 3 Partitionen
- [ ] POST-Request wird akzeptiert (202)
- [ ] Transaktion landet im Topic

### ✅ Aufgabe 2: Fraud-Alert-Service
- [ ] Gültige Transaktionen landen in valid-transactions
- [ ] Hohe Beträge (>10k) landen in fraud-alerts
- [ ] Verdächtige Länder (NG, KP, RU) landen in fraud-alerts

### ✅ Aufgabe 3: Notification-Service
- [ ] Gültige Transaktionen werden mit INFO geloggt
- [ ] Fraud Alerts werden mit WARN geloggt

### ✅ Aufgabe 4: Transfer-Service
- [ ] Konten werden erstellt mit 1000.00 Startguthaben
- [ ] Kontostände werden korrekt aktualisiert
- [ ] Transaktionen werden in DB gespeichert

### ✅ Aufgabe 5: Skalierung
- [ ] 3 Transfer-Service-Instanzen laufen
- [ ] Alle nutzen die gleiche Consumer Group
- [ ] Load wird auf 3 Instanzen verteilt
- [ ] Keine doppelte Verarbeitung durch Load Balancing

### ✅ Aufgabe 6: Idempotenz
- [ ] Doppelte Transaktionen werden erkannt
- [ ] Kontostände werden nur einmal aktualisiert
- [ ] Transaktion existiert nur einmal in DB
- [ ] Warning-Log bei Duplikat

---

## Nützliche Befehle Cheat Sheet

```bash
# Alles starten
docker-compose up -d

# Logs verfolgen
docker logs <service-name> -f

# In PostgreSQL einloggen
docker exec -it transfer-db psql -U transferuser -d transferdb

# Kafka Topics auflisten
docker exec -it kafka-broker kafka-topics --bootstrap-server localhost:9092 --list

# Consumer Groups anzeigen
docker exec -it kafka-broker kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Alles stoppen
docker-compose down

# Alles stoppen + Daten löschen
docker-compose down -v
```

---

Viel Erfolg beim Testen! 🚀
