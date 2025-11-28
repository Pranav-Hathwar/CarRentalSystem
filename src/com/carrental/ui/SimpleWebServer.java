package com.carrental.ui;

import com.carrental.model.Car;
import com.carrental.model.Booking;
import com.carrental.service.VehicleService;
import com.carrental.service.BookingService;
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
    private static final int PORT = 8081;
    private static final String WEB_ROOT = "web";
    private final VehicleService vehicleService;
    private final BookingService bookingService;

    public SimpleWebServer(VehicleService vehicleService, BookingService bookingService) {
        this.vehicleService = vehicleService;
        this.bookingService = bookingService;
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoints
        server.createContext("/api/cars", new CarsHandler());
        server.createContext("/api/bookings", new BookingsHandler());

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
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                handleDeleteCar(exchange);
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
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String body = br.lines().collect(Collectors.joining("\n"));

            try {
                String make = extractJsonValue(body, "make");
                String model = extractJsonValue(body, "model");
                double price = Double.parseDouble(extractJsonValue(body, "price"));
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

        private void handleDeleteCar(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                try {
                    int id = Integer.parseInt(query.substring(3));
                    boolean deleted = vehicleService.deleteCar(id);
                    if (deleted) {
                        sendResponse(exchange, 200, "Car deleted successfully");
                    } else {
                        sendResponse(exchange, 404, "Car not found");
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "Invalid ID format");
                }
            } else {
                sendResponse(exchange, 400, "Missing ID parameter");
            }
        }
    }

    private class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetBookings(exchange);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                handleCreateBooking(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        private void handleGetBookings(HttpExchange exchange) throws IOException {
            List<Booking> bookings = bookingService.getAllBookings();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < bookings.size(); i++) {
                Booking b = bookings.get(i);
                // Manually constructing JSON to avoid dependencies
                json.append(String.format(
                        "{\"id\":%d, \"carId\":%d, \"customerName\":\"%s\", \"pickup\":\"%s\", \"dropoff\":\"%s\", \"total\":%.2f, \"status\":\"%s\", \"date\":\"%s\"}",
                        b.getId(), b.getCarId(), b.getCustomerName(), b.getPickupTime(), b.getDropoffTime(),
                        b.getTotalPrice(), b.getStatus(), b.getBookingDate()));

                // We need to fetch car details to display name in frontend,
                // or frontend fetches car details.
                // Frontend expects 'car' object nested or we change frontend.
                // Let's change frontend to handle carId or enrich here.
                // Enriching here is easier for frontend compatibility if we want to keep
                // frontend simple.
                // But wait, frontend expects `booking.car.name`.
                // I'll enrich the JSON with car name.

                Car car = vehicleService.findCarById(b.getCarId()).orElse(null);
                String carName = car != null ? car.getMake() + " " + car.getModel() : "Unknown Car";

                // Re-doing JSON to include car object structure expected by frontend
                // Frontend: booking.car.name
                // So: "car": { "name": "Toyota Camry" }

                // Let's rewrite the JSON construction
            }
            // Actually, let's do it properly in the loop
            json = new StringBuilder("[");
            for (int i = 0; i < bookings.size(); i++) {
                Booking b = bookings.get(i);
                Car car = vehicleService.findCarById(b.getCarId()).orElse(null);
                String carName = car != null ? car.getMake() + " " + car.getModel() : "Unknown Car";

                json.append("{");
                json.append("\"id\":").append(b.getId()).append(",");
                json.append("\"car\":{\"name\":\"").append(carName).append("\"},");
                json.append("\"pickup\":\"").append(b.getPickupTime()).append("\",");
                json.append("\"dropoff\":\"").append(b.getDropoffTime()).append("\",");
                json.append("\"total\":").append(b.getTotalPrice()).append(",");
                json.append("\"status\":\"").append(b.getStatus()).append("\",");
                json.append("\"date\":\"").append(b.getBookingDate()).append("\"");
                json.append("}");

                if (i < bookings.size() - 1)
                    json.append(",");
            }
            json.append("]");

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, json.toString());
        }

        private void handleCreateBooking(HttpExchange exchange) throws IOException {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String body = br.lines().collect(Collectors.joining("\n"));

            try {
                // Parse JSON manually
                int carId = Integer.parseInt(extractJsonValue(body, "carId"));
                String customerName = extractJsonValue(body, "customerName");
                String pickupTime = extractJsonValue(body, "pickupTime");
                String dropoffTime = extractJsonValue(body, "dropoffTime");
                double totalPrice = Double.parseDouble(extractJsonValue(body, "totalPrice"));

                Booking booking = new Booking(0, carId, customerName, pickupTime, dropoffTime, totalPrice);
                bookingService.createBooking(booking);

                sendResponse(exchange, 201, "Booking created successfully");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Error creating booking: " + e.getMessage());
            }
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
