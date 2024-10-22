package org.nextples.places.server.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.nextples.places.controller.ApiController;
import org.nextples.places.controller.HtmlController;
import org.nextples.places.model.Location;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SuggestionsHandler implements HttpHandler {
    private final ApiController apiController;

    public SuggestionsHandler(ApiController apiController) {
        this.apiController = apiController;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            String place = body.split("=")[1];
            sendResponse(exchange, place);
        }
    }

    private void sendResponse(HttpExchange exchange, String place) {
        apiController.getLocationSuggestions(place).thenAccept(suggestions -> {
            try {
                String htmlPage = HtmlController.getPage("html/suggestions.html");
                StringBuilder suggestionsBuilder = new StringBuilder();

                for (Location location : suggestions) {
                    suggestionsBuilder.append("<input type='radio' name='location' value='")
                            .append(location.getLat()).append(",").append(location.getLon()).append("'>")
                            .append(location.getName()).append(" (").append(location.getCountry()).append(")")
                            .append("<br>");
                }
                String response = htmlPage.replace("%suggestions%", suggestionsBuilder.toString());

                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }
}
