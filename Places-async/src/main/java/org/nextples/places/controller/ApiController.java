package org.nextples.places.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nextples.places.model.Location;
import org.nextples.places.model.Place;
import org.nextples.places.model.Weather;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ApiController {
    private static final String GRAPH_HOPPER_API_KEY = "08b18726-a252-44b3-9566-d2845a765581";
    private static final String OPEN_WEATHER_API_KEY = "5068df3280445b9e111694607160914d";
    private static final String OPEN_TRIP_MAP_API_KEY = "5ae2e3f221c38a28845f05b635e72cb0da967e10757f1069be5c2ac8";

    private final HttpClient client;

    public ApiController(HttpClient client) {
        this.client = client;
    }

    public CompletableFuture< ArrayList<Location> > getLocationSuggestions(String place) {
        String encodedPlace = URLEncoder.encode(place, StandardCharsets.UTF_8);
        String url = "https://graphhopper.com/api/1/geocode?q=" + encodedPlace + "&locale=ru&limit=5&key=" + GRAPH_HOPPER_API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.body());
                        ArrayList<Location> locations = new ArrayList<>();

                        for (JsonNode hit : root.get("hits")) {
                            String name = hit.get("name").asText();
                            String country = hit.get("country").asText();
                            double lat = hit.get("point").get("lat").asDouble();
                            double lon = hit.get("point").get("lng").asDouble();
                            locations.add(new Location(name, lat, lon, country));
                        }
                        return locations;
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing Graphhopper data", e);
                    }
                });
    }

    public CompletableFuture<Weather> getWeather(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&units=metric&appid=" + OPEN_WEATHER_API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.body());

                        String locationName = root.get("name").asText();
                        String description = root.get("weather").get(0).get("description").asText();
                        double temperature = root.get("main").get("temp").asDouble();

                        return new Weather(locationName, description, temperature);
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing OpenWeather data", e);
                    }
                });
    }

    public CompletableFuture<ArrayList<Place>> getInterestingPlaces(double lat, double lon, int radius) {
        String url = "https://api.opentripmap.com/0.1/en/places/radius?radius=" + radius + "&lon=" + lon + "&lat=" + lat + "&apikey=" + OPEN_TRIP_MAP_API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.body());
                        ArrayList<Place> places = new ArrayList<>();

                        for (JsonNode feature : root.get("features")) {
                            JsonNode properties = feature.get("properties");

                            String name = properties.get("name").asText();
                            String xid = properties.get("xid").asText();
                            double placeLon = feature.get("geometry").get("coordinates").get(0).asDouble();
                            double placeLat = feature.get("geometry").get("coordinates").get(1).asDouble();

                            places.add(new Place(xid, name, placeLat, placeLon));
                        }
                        return places;
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing OpenTripMap data", e);
                    }
                });
    }

    public CompletableFuture<String> getInterestingPlacesDescription(String xid) {
        String url = "https://api.opentripmap.com/0.1/en/places/xid/" + xid + "?apikey=" + OPEN_TRIP_MAP_API_KEY;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode root = mapper.readTree(response.body());

                        // Проверяем наличие поля 'descr'
                        if (root.has("descr")) {
                            return root.get("descr").asText(); // Возвращаем значение поля descr
                        }

                        // Проверяем наличие поля 'wikipedia_extracts' и его вложенного поля 'text'
                        if (root.has("wikipedia_extracts") && root.get("wikipedia_extracts").has("text")) {
                            return root.get("wikipedia_extracts").get("text").asText(); // Возвращаем текст из Wikipedia
                        }

                        // Если ни одно из полей не найдено, возвращаем сообщение об отсутствии информации
                        return "No description available";
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing place details from OpenTripMap", e);
                    }
                });
    }

}
