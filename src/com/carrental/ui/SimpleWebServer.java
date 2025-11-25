package com.carrental.ui;

import com.carrental.model.Car;
import com.carrental.service.VehicleService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleWebServer {
    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";
    private final VehicleService vehicleService;

    public SimpleWebServer(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/cars", new CarsHandler());

        // Static File Handler (Catch-all)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on http://localhost:" + PORT);
        System.out.println("Serving files from: " + Paths.get(WEB_ROOT).toAbsolutePath());
    }

    private class CarsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetCars(exchange);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                handleAddCar(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        private void handleGetCars(HttpExchange exchange) throws IOException {
            List<Car> cars = vehicleService.getAllCars();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < cars.size(); i++) {
                Car car = cars.get(i);
                json.append(String.format(
                        "{\"id\":%d, \"name\":\"%s %s\", \"price\":%.2f, \"image\":\"%s\", \"features\":[\"Standard\"]}",
                        car.getId(), car.getMake(), car.getModel(), car.getBasePricePerDay(),
                        car.getImagePath() != null ? car.getImagePath() : "images/default.jpg"));
                if (i < cars.size() - 1)
                    json.append(",");
            }
            json.append("]");

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, json.toString());
        }

        private void handleAddCar(HttpExchange exchange) throws IOException {
            // Simple multipart/form-data parser would be needed here for real file uploads.
            // For simplicity in this "nothing extra" constraint, we'll assume a simple JSON
            // payload
            // or just basic form fields if we were using a library.
            // BUT, the user wants to "add photo".
            // To keep it "simple" and "clean" without external libraries (like Apache
            // Commons FileUpload),
            // we will implement a very basic parser or ask the user to provide a URL/Path
            // for now?
            // Wait, the requirement is "accept jpeg format only".
            // Let's try to parse a simple JSON body with base64 image or just file path if
            // running locally.
            // Given it's a local app, let's accept a JSON body with details.

            // Actually, let's stick to the prompt: "add car manually... add the photo...
            // jpeg only".
            // We'll read the input stream.

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String body = br.lines().collect(Collectors.joining("\n"));

            // Very naive JSON parsing for this specific task to avoid dependencies
            try {
                String make = extractJsonValue(body, "make");
                String model = extractJsonValue(body, "model");
                double price = Double.parseDouble(extractJsonValue(body, "price"));
                String imageType = extractJsonValue(body, "imageType"); // "url" or "base64"
                String imageData = extractJsonValue(body, "image");

                if (make == null || model == null) {
                    sendResponse(exchange, 400, "Missing data");
                    return;
                }

                String imagePath = "images/default.jpg";
                if (imageData != null && !imageData.isEmpty()) {
                    if (!imageData.startsWith("data:image/jpeg") && !imageData.startsWith("data:image/jpg")) {
                        sendResponse(exchange, 400, "Only JPEG images are allowed.");
                        return;
                    }

                    // Save base64 image
                    String base64Image = imageData.split(",")[1];
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
                    String fileName = "car_" + System.currentTimeMillis() + ".jpg";
                    Path destinationFile = Paths.get(WEB_ROOT, "images", fileName);
                    Files.write(destinationFile, imageBytes);
                    imagePath = "images/" + fileName;
                }

                int newId = vehicleService.getAllCars().size() + 1;
                Car newCar = new Car(newId, make, model, price, imagePath);
                vehicleService.addCar(newCar);

                sendResponse(exchange, 201, "Car added successfully");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int start = json.indexOf(pattern);
            if (start == -1) {
                // Try number/boolean (no quotes)
                pattern = "\"" + key + "\":";
                start = json.indexOf(pattern);
                if (start == -1)
                    return null;
                start += pattern.length();
                int end = json.indexOf(",", start);
                if (end == -1)
                    end = json.indexOf("}", start);
                return json.substring(start, end).trim();
            }
            start += pattern.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path))
                path = "/index.html";

            Path file = Paths.get(WEB_ROOT, path);
            if (Files.exists(file) && !Files.isDirectory(file)) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                sendResponse(exchange, 404, "File Not Found");
            }
        }

        private String getContentType(String path) {
            if (path.endsWith(".html"))
                return "text/html";
            if (path.endsWith(".css"))
                return "text/css";
            if (path.endsWith(".js"))
                return "application/javascript";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
                return "image/jpeg";
            if (path.endsWith(".png"))
                return "image/png";
            return "application/octet-stream";
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}
