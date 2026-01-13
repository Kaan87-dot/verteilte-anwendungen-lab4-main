# Komplette Anleitung: Aufgaben 1-6 beenden (Windows CMD)

## 📋 Übersicht: Was musst du tun?

Alle 6 Aufgaben sind bereits **vollständig implementiert**! Du musst nur noch:
1. Das System starten
2. Die Tests durchführen
3. Die Ergebnisse überprüfen

---

## 🚀 SCHRITT 1: System starten

### 1.1 Projekt bauen
```cmd
REM Wechsle ins Projektverzeichnis (passe den Pfad an!)
cd C:\Pfad\zu\verteilte-anwendungen-lab4-main

REM Maven Build (dauert ca. 2-3 Minuten)
mvn clean package -DskipTests

REM Docker Images bauen (dauert ca. 5-10 Minuten)
docker-compose build
```

**Erwartete Ausgabe:**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### 1.2 System starten
```cmd
REM Alle Services starten
docker-compose up -d

REM 60 Sekunden warten bis alles hochgefahren ist
timeout /t 60 /nobreak

REM Status prüfen - alle Services sollten "Up" sein
docker-compose ps
```

**Erwartete Ausgabe:**
```
NAME                    STATUS    PORTS
kafka-broker            Up        0.0.0.0:9092->9092/tcp
transaction-service     Up        0.0.0.0:8081->8081/tcp
fraud-alert-service     Up        0.0.0.0:8083->8083/tcp
notification-service    Up        0.0.0.0:8085->8085/tcp
transfer-service-1      Up        0.0.0.0:8084->8084/tcp
transfer-service-2      Up        0.0.0.0:8086->8086/tcp
transfer-service-3      Up        0.0.0.0:8087->8087/tcp
transfer-db             Up        0.0.0.0:5432->5432/tcp
```

**Hinweis:** Die Container-Namen können je nach Docker-Compose Version ein Präfix haben (z.B. `verteilte-anwendungen-lab4-kafka-broker-1`).

### 1.3 Kafka Topics erstellen

Da Auto-Create deaktiviert ist, müssen die Topics manuell erstellt werden:

**Option 1: Automatisches Script (empfohlen)**
```cmd
REM Führe das bereitgestellte Script aus
create-topics.cmd
```

Das Script findet automatisch den Kafka-Container und erstellt alle benötigten Topics.

**Option 2: Manuelle Erstellung**
```cmd
REM Finde zuerst den genauen Namen des Kafka-Containers
docker ps | findstr kafka-broker

REM Verwende den Containernamen (z.B. verteilte-anwendungen-lab4-kafka-broker-1)
REM Erstelle die Topics (ersetze CONTAINER_NAME mit deinem Containernamen):

REM Topic: raw-transactions (3 Partitionen)
docker exec -it CONTAINER_NAME /opt/kafka/bin/kafka-topics.sh --create --topic raw-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

REM Topic: valid-transactions (3 Partitionen)
docker exec -it CONTAINER_NAME /opt/kafka/bin/kafka-topics.sh --create --topic valid-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

REM Topic: fraud-alerts (3 Partitionen)
docker exec -it CONTAINER_NAME /opt/kafka/bin/kafka-topics.sh --create --topic fraud-alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

REM Prüfe ob Topics erstellt wurden
docker exec -it CONTAINER_NAME /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

**Beispiel mit vollständigem Namen:**
```cmd
docker exec -it verteilte-anwendungen-lab4-kafka-broker-1 /opt/kafka/bin/kafka-topics.sh --create --topic raw-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it verteilte-anwendungen-lab4-kafka-broker-1 /opt/kafka/bin/kafka-topics.sh --create --topic valid-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
docker exec -it verteilte-anwendungen-lab4-kafka-broker-1 /opt/kafka/bin/kafka-topics.sh --create --topic fraud-alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

**Erwartete Ausgabe:**
```
Created topic raw-transactions.
Created topic valid-transactions.
Created topic fraud-alerts.
```

---

## ✅ AUFGABE 1: Topic-Erstellung raw-transactions (1 Punkt)

### Was ist zu tun?
✅ **Bereits erledigt!** Topic ist konfiguriert mit 3 Partitionen und 7 Tage Retention.

### Wie testen?

#### Test 1: Transaktion senden
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"DEAcc1\", \"toAccount\": \"DEAcc2\", \"amount\": 100.50, \"currency\": \"EUR\"}"
```

**Erwartete Ausgabe:**
```json
{
  "transactionId": "tx-abc123...",
  "fromAccount": "DEAcc1",
  "toAccount": "DEAcc2",
  "amount": 100.5,
  "currency": "EUR",
  "timestamp": "2026-01-13T08:30:00Z"
}
```
HTTP Status: **202 Accepted**

#### Test 2: Topic im Kafka UI überprüfen
1. Öffne Browser: **http://localhost:8080**
2. Navigiere zu **Topics** → **raw-transactions**
3. Klicke auf **Messages**

**Erwartete Ausgabe:**
- Topic existiert ✅
- Hat 3 Partitionen ✅
- Enthält deine Transaktion ✅

#### Test 3: Konfiguration überprüfen
```cmd
REM Verwende den Containernamen aus docker ps (z.B. verteilte-anwendungen-lab4-kafka-broker-1)
docker exec -it verteilte-anwendungen-lab4-kafka-broker-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic raw-transactions
```

**Alternative:** Falls dein Container einen anderen Namen hat:
```cmd
REM Finde den genauen Namen
docker ps | findstr kafka-broker

REM Verwende dann den angezeigten Namen
docker exec -it IHR_CONTAINER_NAME /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic raw-transactions
```

**Erwartete Ausgabe:**
```
Topic: raw-transactions
PartitionCount: 3
ReplicationFactor: 1
```

### ✅ Lösung für Aufgabe 1:
- **Partitionen**: 3 (für parallele Verarbeitung)
- **Retention**: 7 Tage (Standard, für Replay)
- **Begründung**: 3 Partitionen ermöglichen Skalierung, 7 Tage ausreichend für Fehlerbehandlung

---

## ✅ AUFGABE 2: Fraud-Alert-Service (1.5 Punkte)

### Was ist zu tun?
✅ **Bereits erledigt!** 
- HighAmountStrategy implementiert (Beträge > 10.000 EUR)
- Im FraudCheckService registriert
- FraudAlertProducer sendet an fraud-alerts Topic
- ValidTransactionProducer sendet an valid-transactions Topic

### Wie testen?

#### Test 1: Gültige Transaktion (sollte NICHT als Fraud erkannt werden)
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"DEAcc1\", \"toAccount\": \"DEAcc2\", \"amount\": 500.00, \"currency\": \"EUR\"}"
```

**Überprüfen:**
```cmd
REM Logs des Fraud-Alert-Service (letzte 20 Zeilen)
docker logs fraud-alert-service --tail 20
```

**Erwartete Ausgabe:**
```
INFO Valid Transaction sent: Transaction{fromAccount='DEAcc1', toAccount='DEAcc2', amount=500.0}
```

**Im Kafka UI:**
- Topic **valid-transactions** enthält die Transaktion ✅
- Topic **fraud-alerts** enthält sie NICHT ✅

#### Test 2: Fraud durch hohen Betrag (> 10.000)
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"DEAcc1\", \"toAccount\": \"DEAcc2\", \"amount\": 15000.00, \"currency\": \"EUR\"}"
```

**Überprüfen:**
```cmd
docker logs fraud-alert-service --tail 20
```

**Erwartete Ausgabe:**
```
WARN Fraud Alert sent: HIGH_AMOUNT for account DEAcc1
```

**Im Kafka UI:**
- Topic **fraud-alerts** enthält den Alert ✅
- Topic **valid-transactions** enthält sie NICHT ✅

#### Test 3: Fraud durch verdächtiges Land (NG, KP, RU)
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"NGAcc1\", \"toAccount\": \"DEAcc2\", \"amount\": 500.00, \"currency\": \"EUR\"}"
```

**Erwartete Ausgabe:**
```
WARN Fraud Alert sent: SUSPICIOUS_LOCATION for account NGAcc1
```

### ✅ Lösung für Aufgabe 2:
- **HighAmountStrategy**: Prüft Betrag > 10.000 EUR
- **Registrierung**: In FraudCheckService.java im Konstruktor
- **Topics**: valid-transactions und fraud-alerts konfiguriert
- **Funktionsweise**: Strategy Pattern ermöglicht einfache Erweiterung

---

## ✅ AUFGABE 3: Notification-Service (2 Punkte)

### Was ist zu tun?
✅ **Bereits erledigt!**
- Consumer für valid-transactions (LOG.info)
- Consumer für fraud-alerts (LOG.warn)

### Wie testen?

#### Test 1: Gültige Transaktion senden
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"TestAcc1\", \"toAccount\": \"TestAcc2\", \"amount\": 250.00, \"currency\": \"EUR\"}"

REM Warten (5 Sekunden)
timeout /t 5 /nobreak

REM Logs prüfen
docker logs notification-service | findstr "Valid Transaction"
```

**Erwartete Ausgabe:**
```
INFO ✓ Valid Transaction: tx-... | From: TestAcc1 | To: TestAcc2 | Amount: 250.0 EUR
```

#### Test 2: Fraud Alert senden
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"TestAcc3\", \"toAccount\": \"TestAcc4\", \"amount\": 20000.00, \"currency\": \"EUR\"}"

REM Warten
timeout /t 5 /nobreak

REM Logs prüfen
docker logs notification-service | findstr "FRAUD ALERT"
```
    "currency": "EUR"
  }'

# Warten
sleep 5

# Logs prüfen
docker logs notification-service | grep "FRAUD ALERT"
```

**Erwartete Ausgabe:**
```
WARN ⚠ FRAUD ALERT: HIGH_AMOUNT | Alert ID: ... | Account: TestAcc3 | Amount: 20000.0 | Timestamp: ...
```

### ✅ Lösung für Aufgabe 3:
- **Consumer Groups**: notification-group (eine Gruppe für beide Topics)
- **Partitionen**: 3 pro Topic (konsistent mit anderen Topics)
- **Log-Level**: INFO für valid, WARN für fraud
- **Begründung**: Eine Gruppe ausreichend, da Logging schnell ist

---

## ✅ AUFGABE 4: Transaktionsverarbeitung Transfer-Service (2 Punkte)

### Was ist zu tun?
✅ **Bereits erledigt!**
- Konten werden erstellt mit 1000.00 Startguthaben
- Kontostände werden korrekt aktualisiert
- Transaktionen werden in PostgreSQL gespeichert

### Wie testen?

#### Test: Transaktion durchführen
```cmd
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"VerifAcc1\", \"toAccount\": \"VerifAcc2\", \"amount\": 200.00, \"currency\": \"EUR\"}"

REM Warten
timeout /t 5 /nobreak

REM Logs einer Transfer-Service Instanz prüfen
docker logs transfer-service-1 | findstr "VerifAcc"
docker logs transfer-service-2 | findstr "VerifAcc"
docker logs transfer-service-3 | findstr "VerifAcc"
```

**Erwartete Ausgabe (in einer der 3 Instanzen):**
```
INFO [transfer-service-group] Received transaction: tx-... from VerifAcc1 to VerifAcc2 amount: 200.0
INFO Updated balance for VerifAcc1: 800.00
INFO Updated balance for VerifAcc2: 1200.00
INFO Transaction saved: tx-...
```

#### Datenbank überprüfen
```cmd
REM In PostgreSQL einloggen
docker exec -it transfer-db psql -U transferuser -d transferdb
```

**SQL-Abfragen in psql:**
```sql
-- Kontostände prüfen
SELECT account_id, balance, created_at, updated_at 
FROM accounts 
WHERE account_id IN ('VerifAcc1', 'VerifAcc2')
ORDER BY account_id;
```

**Erwartete Ausgabe:**
```
 account_id | balance  | created_at              | updated_at
------------+----------+-------------------------+-------------------------
 VerifAcc1  |  800.00  | 2026-01-13 08:35:00     | 2026-01-13 08:35:00
 VerifAcc2  | 1200.00  | 2026-01-13 08:35:00     | 2026-01-13 08:35:00
(2 rows)
```

**Erklärung:**
- VerifAcc1: Startguthaben 1000.00 - 200.00 = 800.00 ✅
- VerifAcc2: Startguthaben 1000.00 + 200.00 = 1200.00 ✅

```sql
-- Transaktionen prüfen
SELECT transaction_id, from_account, to_account, amount, currency, processed_at 
FROM transactions 
WHERE from_account = 'VerifAcc1'
ORDER BY processed_at DESC
LIMIT 5;
```

**Erwartete Ausgabe:**
```
 transaction_id | from_account | to_account | amount | currency | processed_at
----------------+--------------+------------+--------+----------+-------------------------
 tx-...         | VerifAcc1    | VerifAcc2  | 200.00 | EUR      | 2026-01-13 08:35:00
(1 row)
```

```sql
-- Beenden
\q
```

### ✅ Lösung für Aufgabe 4:
- **getOrCreateAccount()**: Erstellt Konto mit 1000.00 falls nicht vorhanden
- **updateBalance()**: Aktualisiert Kontostände (subtract/add)
- **saveTransaction()**: Speichert Transaktion in DB
- **@Transactional**: Garantiert Atomarität (alles oder nichts)

---

## ✅ AUFGABE 5: Skalierung konfigurieren (2 Punkte)

### Was ist zu tun?
✅ **Bereits erledigt!**
- 3 Partitionen für valid-transactions Topic
- Alle 3 Transfer-Service Instanzen nutzen die gleiche Consumer Group
- Load Balancing funktioniert

### Wie testen?

#### Test: Load Balancing überprüfen
```cmd
REM 9 Transaktionen senden (Schleife in CMD)
for /L %%i in (1,1,9) do (
  curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\": \"ScaleAcc%%i\", \"toAccount\": \"TargetAcc\", \"amount\": 100, \"currency\": \"EUR\"}"
  echo  - Transaktion %%i gesendet
  timeout /t 1 /nobreak >nul
)

REM Warten
timeout /t 10 /nobreak

REM Logs aller 3 Instanzen prüfen
echo === Instance 1 ===
docker logs transfer-service-1 | findstr "ScaleAcc" | find /c /v ""

echo === Instance 2 ===
docker logs transfer-service-2 | findstr "ScaleAcc" | find /c /v ""

echo === Instance 3 ===
docker logs transfer-service-3 | findstr "ScaleAcc" | find /c /v ""
```

**Erwartete Ausgabe:**
```
=== Instance 1 ===
3
=== Instance 2 ===
3
=== Instance 3 ===
3
```

**Erklärung:** Jede Instanz hat ca. 33% der Last verarbeitet (3 von 9 Transaktionen)

#### Consumer Group Status überprüfen
```cmd
docker exec -it kafka-broker kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group transfer-service-group
```

**Erwartete Ausgabe:**
```
GROUP                    TOPIC              PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
transfer-service-group   valid-transactions 0          5               5               0
transfer-service-group   valid-transactions 1          4               4               0
transfer-service-group   valid-transactions 2          5               5               0
```

**Erklärung:**
- 3 Consumer (die 3 Instanzen) ✅
- 3 Partitionen (0, 1, 2) ✅
- Jede Instanz hat eine Partition ✅
- LAG = 0 (alle Nachrichten verarbeitet) ✅

### ✅ Lösung für Aufgabe 5:
- **Partitionen**: 3 (ermöglicht bis zu 3 parallele Consumer)
- **Consumer Groups**: 1 Gruppe "transfer-service-group" für alle Instanzen
- **Skalierung**: Load Balancing durch Kafka (jede Instanz bekommt 1 Partition)
- **Reihenfolge**: Garantiert innerhalb einer Partition
- **Keine Duplikate**: Jede Partition wird nur von 1 Consumer gelesen

---

## ✅ AUFGABE 6: Idempotenz implementieren (1.5 Punkte)

### Was ist zu tun?
✅ **Bereits erledigt!**
- transactionId-basierte Deduplizierung
- Check vor jeder Verarbeitung
- Verhindert doppelte Ausführung

### Wie testen?

#### Test: Doppelte Verarbeitung simulieren
```cmd
REM 1. Transaktion mit fester ID senden
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"transactionId\": \"idempotenz-test-999\", \"fromAccount\": \"IdempAcc1\", \"toAccount\": \"IdempAcc2\", \"amount\": 300.00, \"currency\": \"EUR\"}"

timeout /t 5 /nobreak

REM 2. Kontostände VORHER notieren
docker exec -it transfer-db psql -U transferuser -d transferdb -c "SELECT account_id, balance FROM accounts WHERE account_id IN ('IdempAcc1', 'IdempAcc2');"
```

**Erwartete Ausgabe:**
```
 account_id | balance
------------+---------
 IdempAcc1  | 700.00
 IdempAcc2  | 1300.00
(2 rows)
```

**Notiere diese Werte!**

```cmd
REM 3. GLEICHE Transaktion nochmal senden
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"transactionId\": \"idempotenz-test-999\", \"fromAccount\": \"IdempAcc1\", \"toAccount\": \"IdempAcc2\", \"amount\": 300.00, \"currency\": \"EUR\"}"
  -d '{
    "transactionId": "idempotenz-test-999",
    "fromAccount": "IdempAcc1",
    "toAccount": "IdempAcc2",
    "amount": 300.00,
    "currency": "EUR"
  }'

sleep 5

# 4. Kontostände NACHHER prüfen
docker exec -it transfer-db psql -U transferuser -d transferdb \
  -c "SELECT account_id, balance FROM accounts WHERE account_id IN ('IdempAcc1', 'IdempAcc2');"
```

**Erwartete Ausgabe:**
```
 account_id | balance
------------+---------
 IdempAcc1  | 700.00    ← UNVERÄNDERT!
 IdempAcc2  | 1300.00   ← UNVERÄNDERT!
(2 rows)
```

**✅ BEWEIS: Kontostände sind gleich geblieben!**

```cmd
REM 5. Logs prüfen
docker logs transfer-service-1 | findstr "idempotenz-test-999"
docker logs transfer-service-2 | findstr "idempotenz-test-999"
docker logs transfer-service-3 | findstr "idempotenz-test-999"
```

**Erwartete Ausgabe:**
```
INFO [transfer-service-group] Received transaction: idempotenz-test-999 from IdempAcc1 to IdempAcc2 amount: 300.0
INFO Updated balance for IdempAcc1: 700.00
INFO Updated balance for IdempAcc2: 1300.00
INFO Transaction saved: idempotenz-test-999

INFO [transfer-service-group] Received transaction: idempotenz-test-999 from IdempAcc1 to IdempAcc2 amount: 300.0
WARN Transaction idempotenz-test-999 already processed. Skipping.  ← IDEMPOTENZ!
```

#### Duplikat-Check in Datenbank
```cmd
docker exec -it transfer-db psql -U transferuser -d transferdb
```

```sql
-- Prüfe ob Transaktion nur einmal existiert
SELECT transaction_id, COUNT(*) 
FROM transactions 
WHERE transaction_id = 'idempotenz-test-999'
GROUP BY transaction_id;
```

**Erwartete Ausgabe:**
```
 transaction_id       | count
----------------------+-------
 idempotenz-test-999  |   1     ← Nur 1 Eintrag!
(1 row)
```

```sql
\q
```

### ✅ Lösung für Aufgabe 6:
- **Deduplizierung**: Über transactionId
- **Methode**: isTransactionProcessed() prüft DB vor Verarbeitung
- **Atomarität**: Check und Insert in derselben @Transactional
- **Zuverlässigkeit**: DB als Single Source of Truth
- **Resultat**: At-least-once → At-most-once Semantik

---

## 📊 ZUSAMMENFASSUNG: Alle Aufgaben abgeschlossen

| Aufgabe | Punkte | Status | Wie testen? |
|---------|--------|--------|-------------|
| 1: Topic-Erstellung | 1.0 | ✅ Erledigt | Transaktion senden, Kafka UI prüfen |
| 2: Fraud-Alert-Service | 1.5 | ✅ Erledigt | Gültige + Fraud Transaktionen senden |
| 3: Notification-Service | 2.0 | ✅ Erledigt | Logs prüfen (INFO vs WARN) |
| 4: Transaktionsverarbeitung | 2.0 | ✅ Erledigt | DB prüfen (Kontostände + Transaktionen) |
| 5: Skalierung | 2.0 | ✅ Erledigt | 9 Transaktionen senden, Load prüfen |
| 6: Idempotenz | 1.5 | ✅ Erledigt | Gleiche Transaktion 2x senden |
| **GESAMT** | **10.0** | **✅ 100%** | Alle Tests durchführen |

---

## 🎯 KOMPLETTER END-TO-END TEST

Führe diesen Test durch um alles auf einmal zu überprüfen:

```cmd
echo === 1. Gültige Transaktion ===
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\":\"E2E1\",\"toAccount\":\"E2E2\",\"amount\":150,\"currency\":\"EUR\"}"

timeout /t 5 /nobreak

echo === 2. Fraud: Hoher Betrag ===
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\":\"E2E3\",\"toAccount\":\"E2E4\",\"amount\":25000,\"currency\":\"EUR\"}"

timeout /t 5 /nobreak

echo === 3. Fraud: Verdächtiges Land ===
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\":\"KPAcc1\",\"toAccount\":\"E2E5\",\"amount\":500,\"currency\":\"EUR\"}"

timeout /t 5 /nobreak

echo === 4. Gültige Transaktion ===
curl -X POST http://localhost:8081/api/transactions -H "Content-Type: application/json" -d "{\"fromAccount\":\"E2E6\",\"toAccount\":\"E2E7\",\"amount\":750,\"currency\":\"EUR\"}"

timeout /t 10 /nobreak

echo.
echo === ERGEBNISSE ÜBERPRÜFEN ===
echo.
echo --- Notification-Service: Valid Transactions ---
docker logs notification-service | findstr "Valid Transaction" | findstr "E2E"

echo.
echo --- Notification-Service: Fraud Alerts ---
docker logs notification-service | findstr "FRAUD ALERT" | findstr /C:"E2E" /C:"KP"

echo.
echo --- Datenbank: Anzahl verarbeiteter Transaktionen ---
docker exec -it transfer-db psql -U transferuser -d transferdb -c "SELECT COUNT(*) as valid_transactions FROM transactions WHERE from_account LIKE 'E2E%%';"

echo.
echo --- Transfer-Service: Load Balancing ---
REM Zähle Transaktionen pro Instanz
for /f %%a in ('docker logs transfer-service-1 ^| findstr "E2E" ^| find /c /v ""') do echo Instance 1: %%a Transaktionen
for /f %%a in ('docker logs transfer-service-2 ^| findstr "E2E" ^| find /c /v ""') do echo Instance 2: %%a Transaktionen
for /f %%a in ('docker logs transfer-service-3 ^| findstr "E2E" ^| find /c /v ""') do echo Instance 3: %%a Transaktionen
```

**Erwartete Ergebnisse:**
- ✅ 2 gültige Transaktionen in Notification-Service Logs
- ✅ 2 Fraud Alerts in Notification-Service Logs
- ✅ 2 Transaktionen in PostgreSQL Datenbank
- ✅ Load verteilt auf 3 Transfer-Service Instanzen

---

## 📚 WEITERE DOKUMENTATION

- **SUMMARY_GERMAN.md**: Vollständige Erklärung aller Änderungen
- **TESTING_GUIDE.md**: Ausführliche Testanleitung
- **IMPLEMENTATION_DOCUMENTATION.md**: Technische Details und Begründungen
- **QUICK_REFERENCE.md**: Schnellreferenz für häufige Befehle

---

## 🎉 FERTIG!

Wenn alle Tests erfolgreich waren, hast du:
- ✅ Alle 6 Aufgaben vollständig implementiert
- ✅ 10/10 Punkte erreicht
- ✅ Ein produktionsreifes System gebaut
- ✅ Fraud Detection mit Strategy Pattern
- ✅ Horizontale Skalierung mit Kafka
- ✅ Idempotente Transaktionsverarbeitung
- ✅ Vollständige Dokumentation

**Das Projekt ist bereit für die Abgabe! 🚀**
