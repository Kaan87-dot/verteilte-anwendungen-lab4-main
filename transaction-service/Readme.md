# Transaction Service

## Beschreibung

Der **Transaction Service** stellt einen REST-Endpoint bereit, über den neue Transaktionen eingereicht werden können. Gültige Transaktionen werden an das Kafka-Topic `raw-transactions` gesendet.

## Konfiguration

- **Port**: 8081
- **Kafka Topic (Outgoing)**: `raw-transactions`


