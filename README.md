# Verteilte Anwendungen Übungsaufgabe 4

In diesem Projekt implementieren Sie einen verteilten Microservice-Workflow zur Verarbeitung von Transaktionen über Kafka. Die Microservices kommunizieren asynchron über Kafka-Topics und jeder Service hat eine spezifische Aufgabe in der Transaktionsverarbeitungskette.




## Deadlines
   **22.01.2026**


### Microservices

1. **Transaction-Service** (Port 8081)
   - Stellt einen REST-Endpoint bereit, um neue Transaktionen zu empfangen
   - Validiert eingehende Transaktionen
   - Schreibt valid Transaktionen in das `raw-transactions` Topic

2. **Fraud-Alert-Service** (Port 8083)
   - Konsumiert Transaktionen aus `raw-transactions`
   - Prüft jede Transaktion mithilfe des Strategy Patterns auf Fraud
   - Schreibt Fraud-Fälle in das `fraud-alerts` Topic
   - Schreibt valid Transaktionen in das `valid-transactions` Topic

3. **Notification-Service** (Port 8085)
   - Konsumiert aus beiden Topics: `valid-transactions` und `fraud-alerts`
   - Loggt valid Transaktionen mit `LOG.info()`
   - Loggt Fraud Alerts mit `LOG.warn()`

4. **Transfer-Service** (Port 8084/8086/8087, mehrere Instanzen möglich)
   - Konsumiert valid Transaktionen aus `valid-transactions`
   - Führt die eigentliche Geldüberweisung durch (Konto A → Konto B)
   - Speichert Transaktionen und aktualisiert Kontostände in PostgreSQL

### Datenfluss

```
Transaction-Service 
    ↓ (raw-transactions)
Fraud-Alert-Service
    ├─→ (fraud-alerts) → Notification-Service
    └─→ (valid-transactions) → Transfer-Service
                                ↓
                              Notification-Service
```

### Topic-Zuordnung

| Service                | Schreibt in Topic               | Liest aus Topic                    |
|------------------------|--------------------------------|-----------------------------------|
| Transaction-Service    | `raw-transactions`             | -                                 |
| Fraud-Alert-Service    | `fraud-alerts`, `valid-transactions` | `raw-transactions`              |
| Notification-Service   | -                              | `valid-transactions`, `fraud-alerts` |
| Transfer-Service       | -                              | `valid-transactions`              |

---

## Aufgabenstellung (10 Punkte)

### Aufgabe 1: Topic-Erstellung (1 Punkt)

Erstellen Sie das Kafka-Topic `raw-transactions` und konfigurieren Sie es entsprechend:

1. Entscheiden Sie über die **Anzahl der Partitionen** (Überlegung: Skalierbarkeit, Parallelität)
2. Bestimmen Sie die **Retention-Zeit** (Überlegung: Wie lange sollen Nachrichten gespeichert werden?)
3. **Begründen Sie Ihre Wahl** in der Projektabgabe
4. Testen Sie, ob Transaktionen erfolgreich über den Transaction-Service in das Topic geschrieben werden können

**Beispiel: POST-Request an den Transaction-Service**

Um eine Transaction zu erstellen, senden Sie einen POST-Request an:
```
POST http://localhost:8081/api/transactions
Content-Type: application/json
```

**Transaction JSON-Objekt:**
```json
{
    "transactionId": "tx-12345",
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 100.50,
    "currency": "EUR",
    "timestamp": "2024-01-15T10:30:00Z"
}
```

**Hinweise:**
- `transactionId` und `timestamp` sind optional und werden automatisch generiert, falls nicht angegeben
- `fromAccount` und `toAccount` sind **erforderlich** und dürfen nicht leer sein
- `amount` muss größer als 0 sein
- `currency` sollte angegeben werden (z.B. "EUR")

**Beispiel mit cURL:**
```bash
curl -X POST http://localhost:8081/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "DEAcc1",
    "toAccount": "DEAcc2",
    "amount": 100.50,
    "currency": "EUR"
  }'
```

### Aufgabe 2: Fraud-Alert-Service (1.5 Punkte)

Der Fraud-Alert-Service liest alle Transaktionen aus dem `raw-transactions` Topic und prüft, ob es sich um Fraud handelt.

1. Verstehen Sie den Consumer, der aus dem Topic liest und jede Transaktion prüft, ob es eine Location- oder HighAmount-Strategie ist
2. Die Location-Strategie ist bereits implementiert. Sie müssen nur den Check für High Amount (Betrag > 10000) implementieren
3. Erstellen Sie die beiden Topics, in die der Fraud-Service schreiben muss:
   - Alle valid Transaktionen werden in das `valid-transactions` Topic geschrieben
   - Alle anderen werden in das `fraud-alerts` Topic geschrieben
4. Implementieren Sie die fehlenden Funktionen entsprechend den @TODO-Markierungen im Code
5. Die Fraud-Erkennung nutzt das Strategy Pattern - erweitern Sie die vorhandenen Strategien

### Aufgabe 3: Notification-Service (2 Punkt)

1. Der Service muss beide Topics (`valid-transactions` und `fraud-alerts`) konsumieren
2. Unterschiedliche Log-Level für unterschiedliche Nachrichtentypen verwenden:
   - Valid Transaktionen mit `LOG.info()`
   - Fraud Alerts mit `LOG.warn()`
3. Bestimmen Sie: Anzahl der Partitionen, Consumer Groups und Consumer pro Group
4. **Begründen Sie Ihre Entscheidungen** hinsichtlich Skalierbarkeit und Nachrichtenverteilung

### Aufgabe 4: Transaktionsverarbeitung implementieren (2 Punkte)

Sie haben bisher Kafka als Publisher/Subscriber benutzt. Jetzt wollen wir es als Job Queue einsetzen.

Der Transfer-Service bekommt alle valid Transaktionen und führt die Überweisung durch.

1. Vervollständigen Sie die Transaktionsverarbeitung im `ValidTransactionConsumer`
2. Kontostände müssen korrekt aktualisiert werden:
   - Betrag von Konto A abziehen
   - Betrag zu Konto B hinzufügen
3. Transaktionen müssen auch in der Datenbank persistiert werden (Transaktions-Tabelle)
4. Nutzen Sie die verfügbaren Methoden des `AccountService`

### Aufgabe 5: Skalierung konfigurieren (2 Punkt)

1. Mehrere Instanzen des Transfer-Service können gleichzeitig laufen (3 Instanzen, siehe docker-compose Datei)
2. Entscheiden Sie über die Kafka-Konfiguration:
   - Anzahl der Partitionen im `valid-transactions` Topic
   - Anzahl der Consumer Groups
   - Anzahl der Consumer pro Group
3. **Begründen Sie Ihre Entscheidungen** im Hinblick auf:
   - Skalierbarkeit (wie viele Instanzen können parallel arbeiten?, consumer, consumer groups etc ... )
   - Reihenfolge der Verarbeitung (innerhalb einer Partition muss die Reihenfolge erhalten bleiben, wie stellen wir das sicher?)
4. Auch wenn 3 Instanzen laufen, muss eine Transaktion nur einmal durchgeführt werden, sonst droht, dass das Geld zweimal abgebucht wird

### Aufgabe 6: Idempotenz implementieren (1.5 Punkte)

**Problem**: Mit den vorherigen Aufgaben haben wir garantiert, dass Kafka nur "at-least-once" Delivery bietet - Nachrichten können mehrfach ankommen und mehrfach verarbeitet werden. Für kritische Systeme müssen wir noch eine Sache implementieren, um sicherzustellen, dass wirklich die Transaktion nur einmal ausgeführt wird (z.B. wenn ein Consumer ausgefallen ist oder so).

**Aufgabe**: Schlagen Sie eine Lösung für Idempotenz vor.

1. Mögliche Ansätze (wahlweise oder kombiniert):
   - Deduplizierung über `transactionId`
   - Idempotente Datenbank-Updates
   - Prüfung, ob Transaktion bereits verarbeitet wurde
2. Implementieren Sie Ihre gewählte Lösung
3. **Begründen Sie Ihre gewählte Lösung** und erklären Sie, warum sie zuverlässig ist

---

> **Hinweis:** 
> - Jeder Microservice hat eine eigene Readme-Datei mit weiteren Details und @TODO-Markierungen im Code
> - Ein End-to-End-Integrationstest steht zur Verfügung, um die Funktionalität zu überprüfen
> - Nutzen Sie Docker Compose, um alle Services gemeinsam zu starten. Das Projekt lässt sich mit `docker-compose build` bauen und mit `docker-compose up` starten
