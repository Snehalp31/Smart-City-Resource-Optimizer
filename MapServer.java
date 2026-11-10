import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** Local-only HTTP bridge for the OpenStreetMap/Leaflet operations map. */
public final class MapServer {
    private static HttpServer server;
    private MapServer() {}

    public static synchronized void start() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8765), 0);
        server.createContext("/api/locations", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"Method not allowed\"}");
                return;
            }
            respond(exchange, 200, "application/json; charset=utf-8", MainGUIPastel.mapDataJson());
        });
        server.createContext("/api/route", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "application/json; charset=utf-8", "{\"error\":\"Method not allowed\"}");
                return;
            }
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String area = query.getOrDefault("area", "").trim();
            String type = query.getOrDefault("type", "").trim();
            if (area.isEmpty() || type.isEmpty()) {
                respond(exchange, 400, "application/json; charset=utf-8", "{\"error\":\"area and type are required\"}");
                return;
            }
            respond(exchange, 200, "application/json; charset=utf-8", MainGUIPastel.mapRouteJson(area, type));
        });
        server.createContext("/map", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            Path html = Path.of("web", "map.html");
            if (!Files.isRegularFile(html)) {
                respond(exchange, 404, "text/plain; charset=utf-8",
                        "Missing web/map.html. Run the application from the project root.");
                return;
            }
            try {
                String page = Files.readString(html, StandardCharsets.UTF_8);
                respond(exchange, 200, "text/html; charset=utf-8", page);
            } catch (IOException ex) {
                respond(exchange, 500, "text/plain; charset=utf-8", "Could not read map page.");
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "smart-city-map-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(MapServer::stop, "smart-city-map-shutdown"));
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new HashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) return values;
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            values.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return values;
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
