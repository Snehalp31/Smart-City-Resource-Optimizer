# Smart City Emergency Resource Optimizer

A Java 17 desktop application designed to help manage emergency resources across different areas of a city. It helps municipal staff handle emergency requests, find nearby available resources, manage their allocation and monitor ongoing incidents.

The project uses Java Swing for the desktop interface, Dijkstra's algorithm for finding the shortest paths and JDBC for data storage. It also includes an optional OpenStreetMap-based live operations map.

## Features

- **Two separate portals:** Citizens can register and submit emergency requests, while municipal staff can manage resources and handle incidents.
- **User authentication:** Passwords are secured using salted PBKDF2-HMAC-SHA256 hashing.
- **City area management:** Maintains a weighted, bidirectional graph representing connections between city areas.
- **Shortest path calculation:** Uses Dijkstra's algorithm to find the nearest available resource based on graph distance.
- **Emergency request management:** Records requests with unique IDs, descriptions, locations, priorities and timestamps.
- **Request tracking:** Supports Pending, Assigned, In Progress, Completed and Cancelled statuses.
- **Resource management:** Municipal staff can add, update and remove resources. Resources involved in active requests cannot be removed.
- **Request search and reassignment:** Staff can search requests, reassign resources and view activity history.
- **Reports and analytics:** Displays request statistics, priorities, resource utilization and resource types. Reports can also be exported as CSV files.
- **Database support:** Uses SQLite by default, with optional MySQL support. A local text-file storage option is also available.
- **Live operations map:** Displays configured city areas, emergency requests and available resources using Leaflet and OpenStreetMap.
- **Route visualization:** Shows suggested resource routes on the map using the same Dijkstra implementation used by the Java application.
- **Weather information:** Retrieves current weather conditions for configured areas using the Open-Meteo API.

## Technologies Used

- Java 17
- Java Swing
- JDBC
- SQLite / MySQL
- Maven
- HTML, CSS and JavaScript
- Leaflet.js
- OpenStreetMap
- Open-Meteo API
- JUnit

## Getting Started

### Requirements

- JDK 17 or later
- Maven 3.9 or later
- Desktop environment for running the Swing application
- Internet connection for the map and weather features
- MySQL 8+ (optional)

### Running the project

Clone the repository:

```bash
git clone https://github.com/Snehalp31/Smart-City-Resource-Optimizer.git
cd Smart-City-Resource-Optimizer
```

Run the tests:

```bash
mvn clean test
```

Start the application:

```bash
mvn exec:java
```

Maven automatically downloads the required dependencies, including the SQLite and MySQL JDBC drivers.

## Initial Administrator Setup

The application supports separate Citizen and Municipal Staff portals.

Citizens can register through the application. Municipal administrator accounts are created through environment variables during the initial setup.

For Windows PowerShell:

```powershell
$env:INITIAL_ADMIN_USER = "municipal_admin"
$env:INITIAL_ADMIN_PASSWORD = "your-unique-password"
mvn exec:java
```

The administrator account is created only if a municipal account does not already exist. Remove the environment variables after the initial setup.

**Note:** Do not use real passwords in the source code or commit them to GitHub.

## Live Operations Map

The application includes a locally hosted map available at:

```text
http://127.0.0.1:8765/map
```

The map uses Leaflet for interactive controls and OpenStreetMap for map tiles.

Users can view configured city areas and emergency locations. Selecting an emergency marker displays suggested available resources and their routes.

The route suggestions are calculated by the Java backend through the `/api/route` endpoint. This endpoint uses the existing Dijkstra implementation to find a suitable resource and return the route for visualization.

A few things to keep in mind:

- Routes follow the configured city graph and area coordinates.
- They do not represent actual turn-by-turn road directions.
- The map does not track vehicles using GPS.
- Viewing a suggested route does not automatically assign or dispatch a resource.
- Map coordinates must be entered manually by an authorized user.

An internet connection is required to load Leaflet and OpenStreetMap tiles. The application does not require a Google Maps API key.

## Database Configuration

### SQLite

SQLite is used automatically for local development when no database URL is configured.

The database is stored at:

```text
data/smartcity.db
```

### MySQL

To use MySQL:

1. Install MySQL 8 or later.
2. Execute `db/schema-mysql.sql`.
3. Create a database user with the required permissions.
4. Configure the following environment variables:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/smartcity?useSSL=true&serverTimezone=UTC"
$env:DB_USER = "smartcity_app"
$env:DB_PASSWORD = "YOUR_DATABASE_PASSWORD"
mvn exec:java
```

The application initializes missing tables and uses prepared statements and database transactions.

### File Storage

If the application is compiled and run without Maven or JDBC drivers, it uses a text-file storage fallback.

## Project Structure

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

## Current Limitations

This project is currently a desktop prototype and has some limitations:

- It is designed for local use rather than simultaneous access by multiple municipal teams.
- City area and incident coordinates need to be entered manually.
- Resource locations use resource-center coordinates rather than live GPS positions.
- Weather information is limited to current conditions and is not an official emergency alert system.
- The application does not currently support account recovery or municipal account approval.
- Analytics does not yet include time-based emergency trends.
- The map displays suggested routes but does not perform automatic dispatch.
- The public OpenStreetMap tile service is suitable for development, but production deployment would require a compliant tile provider or self-hosted tiles.

## Future Improvements

Some improvements that could be added in future versions include:

- GPS-based resource tracking.
- Automatic resource allocation and dispatch notifications.
- Address search and geocoding.
- Emergency trend analysis using historical data.
- Multi-user access for municipal departments.
- Better authentication, account recovery and role management.
- Automated database backups and improved audit logging.

## Tests

The project includes JUnit tests for important application logic, including:

- Dijkstra shortest-path calculation.
- Handling disconnected city areas.
- Resource type matching and availability.
- Password hashing.

Run the tests using:

```bash
mvn test
```
