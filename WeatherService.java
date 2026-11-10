import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Free current-weather integration using Open-Meteo; no API key required for ordinary non-commercial use. */
public final class WeatherService {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
    private WeatherService() {}

    public static final class Weather {
        public final double temperatureC, apparentTemperatureC, relativeHumidity, precipitationMm, windSpeedKmh;
        public final int weatherCode;
        public final String description;
        private Weather(double temperatureC, double apparentTemperatureC, double relativeHumidity,
                        double precipitationMm, double windSpeedKmh, int weatherCode) {
            this.temperatureC = temperatureC;
            this.apparentTemperatureC = apparentTemperatureC;
            this.relativeHumidity = relativeHumidity;
            this.precipitationMm = precipitationMm;
            this.windSpeedKmh = windSpeedKmh;
            this.weatherCode = weatherCode;
            this.description = describe(weatherCode);
        }
    }

    public static Weather fetch(double latitude, double longitude) throws IOException, InterruptedException {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid latitude or longitude.");
        }
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m"
                + "&timezone=auto";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "SmartCityResourceOptimizer/1.0")
                .GET().build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Open-Meteo returned HTTP " + response.statusCode() + ". Check your internet connection or request limits.");
        }
        String json = response.body();
        String current = extractObject(json, "current");
        return new Weather(number(current, "temperature_2m"), number(current, "apparent_temperature"),
                number(current, "relative_humidity_2m"), number(current, "precipitation"),
                number(current, "wind_speed_10m"), (int) number(current, "weather_code"));
    }

    private static String extractObject(String json, String key) throws IOException {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{([^}]*)}").matcher(json);
        if (!m.find()) throw new IOException("Weather response did not contain the '" + key + "' object.");
        return m.group(1);
    }
    private static double number(String json, String key) throws IOException {
        Matcher m = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!m.find()) throw new IOException("Weather response is missing '" + key + "'.");
        return Double.parseDouble(m.group(1));
    }
    private static String describe(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 3) return "Mainly clear / partly cloudy / overcast";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snowfall";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code == 85 || code == 86) return "Snow showers";
        if (code >= 95) return "Thunderstorm";
        return "Weather condition code " + code;
    }
}
