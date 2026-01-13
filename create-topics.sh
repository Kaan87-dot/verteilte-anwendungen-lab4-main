#!/bin/bash
# Script zum Erstellen der Kafka Topics
# Script to create Kafka topics

echo "========================================"
echo "Kafka Topics erstellen / Creating Kafka Topics"
echo "========================================"
echo ""

# Finde den Kafka-Container Namen
# Find the Kafka container name
echo "Suche Kafka-Container..."
KAFKA_CONTAINER=$(docker ps --filter "ancestor=apache/kafka:latest" --format "{{.Names}}" | head -n 1)

if [ -z "$KAFKA_CONTAINER" ]; then
    echo "FEHLER: Kafka-Container nicht gefunden!"
    echo "ERROR: Kafka container not found!"
    echo "Bitte stelle sicher, dass docker-compose up -d ausgeführt wurde."
    echo "Please make sure docker-compose up -d has been executed."
    exit 1
fi

echo "Kafka-Container gefunden: $KAFKA_CONTAINER"
echo ""

# Topic: raw-transactions erstellen
echo "[1/3] Erstelle Topic: raw-transactions (3 Partitionen)..."
docker exec -it $KAFKA_CONTAINER kafka-topics --create --topic raw-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo ""

# Topic: valid-transactions erstellen
echo "[2/3] Erstelle Topic: valid-transactions (3 Partitionen)..."
docker exec -it $KAFKA_CONTAINER kafka-topics --create --topic valid-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo ""

# Topic: fraud-alerts erstellen
echo "[3/3] Erstelle Topic: fraud-alerts (3 Partitionen)..."
docker exec -it $KAFKA_CONTAINER kafka-topics --create --topic fraud-alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo ""

# Liste alle Topics auf
echo "========================================"
echo "Vorhandene Topics / Existing Topics:"
echo "========================================"
docker exec -it $KAFKA_CONTAINER kafka-topics --list --bootstrap-server localhost:9092
echo ""

echo "========================================"
echo "Fertig! / Done!"
echo "========================================"
echo ""
echo "Du kannst jetzt mit den Tests fortfahren."
echo "You can now continue with the tests."
echo ""
