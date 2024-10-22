package org.nextples.places.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.nextples.places.controller.ApiController;
import org.nextples.places.controller.HtmlController;
import org.nextples.places.model.Place;
import org.nextples.places.model.Weather;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LocInfoHandler implements HttpHandler {
    private final ApiController apiController;

    public LocInfoHandler(ApiController apiController) {
        this.apiController = apiController;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String[] params = body.split("=")[1].split("%2C");
            double lat = Double.parseDouble(params[0]);
            double lon = Double.parseDouble(params[1]);

            int radius = 5000;  // Значение по умолчанию - 5 км
            if (body.contains("radius=")) {
                radius = Integer.parseInt(body.split("radius=")[1]);
            }


            sendResponse(exchange, lat, lon, radius);
        }
    }

    private void sendResponse(HttpExchange exchange, double lat, double lon, int radius) {
        CompletableFuture<Weather> weatherFuture = apiController.getWeather(lat, lon);
        CompletableFuture<ArrayList<Place>> placesFuture = apiController.getInterestingPlaces(lat, lon, radius);

        CompletableFuture.allOf(weatherFuture, placesFuture).thenAccept(v -> {
            try {
                Weather weather = weatherFuture.join();
                ArrayList<Place> allPlaces = placesFuture.join();
                ArrayList<Place> places = new ArrayList<>(allPlaces.subList(0, 5));

                List<CompletableFuture<Void>> placeDetailsFutures = new ArrayList<>();
                for (Place place : places) {
                    CompletableFuture<Void> placeDetailsFuture = apiController.getInterestingPlacesDescription(place.getXid())
                            .thenAccept(place::setDescription);
                    placeDetailsFutures.add(placeDetailsFuture);
                }

                CompletableFuture.allOf(placeDetailsFutures.toArray(new CompletableFuture[0])).join();

                String response = HtmlController.getPage("html/location_info.html");
                response = response.replace("{{location_name}}", weather.getLocationName())
                        .replace("{{temperature}}",  String.valueOf(weather.getTemperature()))
                        .replace("{{weather_description}}", weather.getDescription())
                        .replace("{{latitude}}", String.valueOf(lat))
                        .replace("{{longitude}}", String.valueOf(lon));

                StringBuilder placesBuilder = new StringBuilder();
                for (Place place : places) {
                    placesBuilder.append("<li>")
                            .append("<strong>").append(place.getName()).append("</strong><br>")
                            .append("<p>").append(place.getDescription()).append("</p>")
                            .append("</li>");
                }

                response = response.replace("{{places}}", placesBuilder.toString());

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }).exceptionally(ex -> {
            try {
                String errorMessage = "Error occurred while processing the request.";
                exchange.sendResponseHeaders(500, errorMessage.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(errorMessage.getBytes());
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }



}
