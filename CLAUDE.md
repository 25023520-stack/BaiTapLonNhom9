<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **BaiTapLonNhom9** (1354 symbols, 4228 relationships, 116 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/BaiTapLonNhom9/context` | Codebase overview, check index freshness |
| `gitnexus://repo/BaiTapLonNhom9/clusters` | All functional areas |
| `gitnexus://repo/BaiTapLonNhom9/processes` | All execution flows |
| `gitnexus://repo/BaiTapLonNhom9/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->

---

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
