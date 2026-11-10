# Smart City Emergency Resource Optimizer

A Java 17 desktop application for city-area management, weighted road networks, emergency resources, incident requests, allocation, reporting, and an optional OpenStreetMap/Leaflet live operations map.

## What is implemented

- Java Swing operations dashboard with an explicit Citizen / Municipal Staff portal selector, role-specific navigation and a refreshed dashboard layout.
- Citizen self-registration; municipal administrator bootstrap through environment variables.
- PBKDF2-HMAC-SHA256 salted password hashes. Passwords are never stored as plaintext.
- Weighted, bidirectional city graph and Dijkstra shortest-path resource selection.
- Matching resource-type validation, availability checks, and disconnected-node handling.
- Emergency requests with unique IDs, descriptions, coordinates, priority and timestamps.
- Request status workflow: Pending, Assigned, In Progress, Completed and Cancelled.
- Resource add/edit/remove operations (active resources cannot be removed).
- Request search, municipal reassignment, activity history and CSV export.
- Analytics charts calculated from application records (status, priority, resource utilization and type).
- Persistent storage through JDBC: SQLite for local development when available, or MySQL when configured.
- Text-file persistence fallback when the SQLite JDBC driver is not available.
- Local-only HTTP bridge serving live application data to an OpenStreetMap/Leaflet map.
- Dijkstra shortest-path suggestions for available matching resources, drawn over the map using configured city-area coordinates.
- Current weather lookup for configured city areas using the free Open-Meteo forecast API (no API key required for ordinary non-commercial use).

## Important integration boundary

The Java allocation engine uses the configured weighted city graph and Dijkstra's algorithm. When an emergency marker is selected, the Leaflet page calls the local `/api/route` endpoint. That endpoint invokes the Java `CityModel.previewNearest()` method, which shares the application's Dijkstra implementation but does not reserve or change a resource. The returned path is drawn over the map. The map suggestion does **not** dispatch or reassign a resource; the application's existing backend allocation workflow remains authoritative. Route lines follow configured area coordinates, not turn-by-turn road geometry or live traffic.

Area and incident coordinates must be entered by an authorized user. The application does not invent or randomly generate coordinates. Areas without coordinates remain usable by the graph allocator but are omitted from the map.

## Requirements

- JDK 17 or later.
- Maven 3.9+ recommended for SQLite/MySQL JDBC drivers and JUnit tests.
- A desktop environment for the Swing UI.
- Internet connection to load Leaflet and OpenStreetMap map tiles. No Google Maps API key or billing account is required.
- Optional: MySQL 8+ server.

## Run (recommended: Maven)

From the project root:

```bash
mvn clean test
mvn exec:java
```

The first command compiles and runs the JUnit business-logic tests. The second launches the Swing application. Maven includes SQLite JDBC and MySQL Connector/J.

## Run without Maven

This path uses the built-in file persistence fallback because JDBC database drivers are not on the classpath:

```bash
javac Main.java MainGUIPastel.java MapServer.java JdbcStore.java WeatherService.java
java MainGUIPastel
```

Run commands from the project root so the `web/` and `data/` folders resolve correctly.

## First-time municipal administrator setup

The login screen explicitly separates the **Citizen portal** from the **Municipal staff portal**. Public sign-up creates **citizen** accounts only. Municipal staff accounts are provisioned by the administrator. To bootstrap the first municipal administrator, set these environment variables before launching the application:

### Windows PowerShell

```powershell
$env:INITIAL_ADMIN_USER = "municipal_admin"
$env:INITIAL_ADMIN_PASSWORD = "use-a-unique-password-at-least-12-chars"
mvn exec:java
```

The account is created only if there is no municipal account yet. Remove the variables after initial setup. Do not commit real credentials.

If the administrator was not bootstrapped on first launch, set the variables and restart. If a municipal account already exists, bootstrap will not create another.

## Live OpenStreetMap configuration

No Google Maps API key or billing account is needed. The application serves the map locally at `http://127.0.0.1:8765/map`. The page uses Leaflet for interactive map controls and OpenStreetMap standard raster tiles for the basemap. The local Java HTTP service binds only to loopback. Map markers refresh every 15 seconds.

Select **Current Weather** in either portal to retrieve current conditions for a configured area from Open-Meteo. An internet connection is required. Weather data is informational and is not an official emergency alert. Select **Live Operations Map** in the desktop application. Select an emergency marker to compare available matching resources. The local `/api/route` endpoint calls the Java `CityModel.previewNearest()` method, using the same Dijkstra implementation as allocation without reserving a resource. It returns the selected resource, configured route path, and graph distance; the map draws the path through the coordinates of those areas. These are configured graph routes, not turn-by-turn road geometry, live traffic, GPS tracking, or a dispatch action.

The map loads Leaflet from `unpkg.com` and tiles from `tile.openstreetmap.org`, so an internet connection is required. Keep the required OpenStreetMap attribution visible. The public OSM tile service has a usage policy and is not a guaranteed production hosting service; for heavy or production use, choose a compliant tile provider or self-host tiles. See <https://operations.osmfoundation.org/policies/tiles/>.

## Database setup

### SQLite development mode

With Maven dependencies available and no `DB_URL` set, the application uses `data/smartcity.db` automatically.

### MySQL mode

1. Install MySQL 8+.
2. Run `db/schema-mysql.sql` to create the `smartcity` database and tables.
3. Create a least-privilege database user.
4. Set environment variables:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/smartcity?useSSL=true&serverTimezone=UTC"
$env:DB_USER = "smartcity_app"
$env:DB_PASSWORD = "YOUR_DATABASE_PASSWORD"
mvn exec:java
```

The Java application initializes missing tables and uses prepared statements and transactions. Do not commit database credentials. If a database URL is explicitly configured but the connection fails, startup reports an error instead of silently switching stores.

### File fallback

If launched with plain `javac` and no JDBC driver, the application creates local files under `data/`. Runtime data is excluded by `.gitignore`.

## Data and privacy

Runtime data is stored under `data/` and is ignored by Git. Do not upload account records, requests, logs, database files, or exports to a public repository. The map's local JSON endpoint exposes mapped areas, configured weighted road edges, resource status/type and request map details; citizen sessions receive only their own request markers.

## Screenshots

No screenshots are included yet. Capture the running desktop dashboard and the OpenStreetMap/Leaflet operations map, then add them to a `screenshots/` folder.

## Main project files

```text
Smart-City-Resource-Optimizer/
├── Main.java
├── MainGUIPastel.java
├── MapServer.java
├── JdbcStore.java
├── WeatherService.java
├── pom.xml
├── README.md
├── .gitignore
├── .env.example
├── LICENSE
├── db/
│   └── schema-mysql.sql
├── tests/
│   └── ProjectLogicTest.java
├── web/
│   └── map.html
└── data/
    └── .gitkeep
```

## Known limitations / next engineering steps

- The application is a single-user desktop prototype; it is not a dispatch-grade multi-user service.
- Map route suggestions are visualized from the configured graph; backend assignment continues to use the graph's configured road weights.
- Coordinates are entered manually; geocoding and address autocomplete are not included.
- Weather integration retrieves current conditions only; it does not issue official severe-weather warnings or automatically change emergency priorities.
- Resource current GPS position and live vehicle tracking are not implemented; map resource markers use the resource-center coordinates.
- There is no automated account recovery or municipal approval workflow.
- The Swing UI is a desktop prototype; it does not include multi-user web sessions or profile-settings management.
- Analytics currently charts request status, priority, resource utilization and request type; a time-series emergency trend chart is not yet implemented.
- The Swing dashboard is a desktop interface rather than a web-scale multi-user operations console.
- For production, add authenticated API sessions, database migrations, role approval, audit retention controls, automated backup, concurrency/locking around allocation, and end-to-end tests with representative map data and a compliant tile source.

## Tests

`tests/ProjectLogicTest.java` covers weighted shortest path, disconnected resources, resource matching/availability, and password hashing. Run with:

```bash
mvn test
```
