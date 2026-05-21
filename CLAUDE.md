## Project Context

### Overview
**BaiTapLonNhom9** — A Java auction system (group assignment). Client-server architecture using TCP sockets. UI built with JavaFX/FXML. Backend runs a standalone TCP server with MySQL via Docker.

### Stack
- **Language:** Java 25, Kotlin 2.3
- **UI:** JavaFX 21 (FXML + MVC controllers)
- **Networking:** TCP sockets (port 5050), custom JSON payload protocol via Gson
- **Database:** MySQL (via Docker Compose), accessed through `UserDAO` / `Database.java`
- **Build:** Maven, produces `server.jar` and `client.jar`
- **Design patterns:** Factory Method (`ItemFactory`), MVC, client-server separation

### Architecture
```
client/   → JavaFX UI, connects to server via AuctionClient (TCP)
server/   → TCP server, business logic in manager/, data in dao/
model/    → Shared entities (User, Item, Auction, Bid)
common/   → Shared payload DTOs and GsonProvider
```

### Key Entry Points
- `client/Launcher.java` — JavaFX client entry point
- `server/ServerMain.java` — TCP server entry point
- `client/App.java` — picks DEMO vs SEPARATE mode

### Running the Project
```bash
# Server (Docker recommended)
docker compose -f docker.yml up --build

# Client (separate terminal)
mvn -DskipTests -Djavafx.run.jvmArgs="-Dauction.client.mode=SEPARATE -Dauction.server.host=127.0.0.1 -Dauction.server.port=5050" javafx:run
```

### Admin Bootstrap
Set `ADMIN_BOOTSTRAP_PASSWORD` env var before starting server. See `.env.example` for full list of vars.

### Current Branch
Working on `dev` branch. Main branch is `main`.
