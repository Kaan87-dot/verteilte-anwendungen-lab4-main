@echo off
REM Script zum Erstellen der Kafka Topics
REM Script to create Kafka topics

echo ========================================
echo Kafka Topics erstellen / Creating Kafka Topics
echo ========================================
echo.

REM Finde den Kafka-Container Namen
REM Find the Kafka container name
echo Suche Kafka-Container...
for /f "tokens=*" %%i in ('docker ps --filter "ancestor=apache/kafka:latest" --format "{{.Names}}"') do set KAFKA_CONTAINER=%%i

if "%KAFKA_CONTAINER%"=="" (
    echo FEHLER: Kafka-Container nicht gefunden!
    echo ERROR: Kafka container not found!
    echo Bitte stelle sicher, dass docker-compose up -d ausgefuehrt wurde.
    echo Please make sure docker-compose up -d has been executed.
    pause
    exit /b 1
)

echo Kafka-Container gefunden: %KAFKA_CONTAINER%
echo.

REM Topic: raw-transactions erstellen
echo [1/3] Erstelle Topic: raw-transactions (3 Partitionen)...
docker exec %KAFKA_CONTAINER% /opt/kafka/bin/kafka-topics.sh --create --topic raw-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo.

REM Topic: valid-transactions erstellen
echo [2/3] Erstelle Topic: valid-transactions (3 Partitionen)...
docker exec %KAFKA_CONTAINER% /opt/kafka/bin/kafka-topics.sh --create --topic valid-transactions --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo.

REM Topic: fraud-alerts erstellen
echo [3/3] Erstelle Topic: fraud-alerts (3 Partitionen)...
docker exec %KAFKA_CONTAINER% /opt/kafka/bin/kafka-topics.sh --create --topic fraud-alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
echo.

REM Liste alle Topics auf
echo ========================================
echo Vorhandene Topics / Existing Topics:
echo ========================================
docker exec %KAFKA_CONTAINER% /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
echo.

echo ========================================
echo Fertig! / Done!
echo ========================================
echo.
echo Du kannst jetzt mit den Tests fortfahren.
echo You can now continue with the tests.
echo.
pause
