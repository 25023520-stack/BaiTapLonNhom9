# BaiTapLonNhom9 — Agent Instructions

A Java auction system (group assignment). Client-server architecture using TCP sockets. UI built with JavaFX/FXML. Backend runs a standalone TCP server with MySQL via Docker.

## Running the Project

```bash
# Server (Docker recommended)
docker compose -f docker.yml up --build

# Client (separate terminal)
mvn -DskipTests -Djavafx.run.jvmArgs="-Dauction.client.mode=SEPARATE -Dauction.server.host=127.0.0.1 -Dauction.server.port=5050" javafx:run
```
