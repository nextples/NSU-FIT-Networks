package org.nextples.places.server;

import com.sun.net.httpserver.HttpServer;
import org.nextples.places.controller.ApiController;
import org.nextples.places.server.handlers.LocInfoHandler;
import org.nextples.places.server.handlers.RootHandler;
import org.nextples.places.server.handlers.SuggestionsHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

public class MyHttpServer {
    private final HttpServer server;

    public MyHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        ApiController apiController = new ApiController(HttpClient.newHttpClient());
        server.createContext("/", new RootHandler());
        server.createContext("/suggestions", new SuggestionsHandler(apiController));
        server.createContext("/location_info", new LocInfoHandler(apiController));
    }


    public void start() {
        server.setExecutor(null);
        server.start();
    }
}
