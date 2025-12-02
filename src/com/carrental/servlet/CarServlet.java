package com.carrental.servlet;

import com.carrental.dao.CarDAO;
import com.carrental.model.Car;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/cars")
public class CarServlet extends HttpServlet {
    private CarDAO carDAO = new CarDAO();

    // GET: Return all cars
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        List<Car> cars = carDAO.getAllCars();

        // Manual JSON construction with proper escaping
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < cars.size(); i++) {
            Car c = cars.get(i);
            String name = escapeJson(c.getName() != null ? c.getName() : "");
            String image = escapeJson(c.getImage() != null ? c.getImage() : "");
            String features = escapeJson(c.getFeatures() != null ? c.getFeatures() : "");
            String type = escapeJson(c.getType() != null ? c.getType() : "CAR");
            String reg = escapeJson(c.getRegistrationNumber() != null ? c.getRegistrationNumber() : "");
            json.append(
                    String.format(
                            "{\"id\":%d, \"name\":\"%s\", \"price\":%.2f, \"image\":\"%s\", \"features\":\"%s\", \"type\":\"%s\", \"registrationNumber\":\"%s\"}",
                            c.getId(), name, c.getPrice(), image, features, type, reg));
            if (i < cars.size() - 1)
                json.append(",");
        }
        json.append("]");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
    }

    // POST: Add a new car
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();

        // Simple manual JSON parsing (assuming flat JSON)
        String name = extractJsonValue(body, "name");
        String priceStr = extractJsonValue(body, "price");
        String image = extractJsonValue(body, "image");
        String features = extractJsonValue(body, "features");
        String type = extractJsonValue(body, "type");
        String registrationNumber = extractJsonValue(body, "registrationNumber");

        if (name == null || name.isEmpty() || priceStr == null || priceStr.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Name and price are required\"}");
            return;
        }

        // Set default values if not provided
        if (image == null || image.isEmpty()) {
            image = "images/default.jpg";
        }
        if (features == null || features.isEmpty()) {
            features = "Standard Features";
        }
        if (type == null || type.isEmpty()) {
            type = "CAR";
        }
        if (registrationNumber == null) {
            registrationNumber = "";
        }

        try {
            Car car = new Car(0, name, Double.parseDouble(priceStr), image, features, type, registrationNumber);
            if (carDAO.addCar(car)) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true, \"message\":\"Car added successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to add car\"}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid price format\"}");
        }
    }

    // DELETE: Delete a car
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        String idStr = req.getParameter("id");
        if (idStr != null) {
            int id = Integer.parseInt(idStr);
            if (carDAO.deleteCar(id)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\":\"Car deleted\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;
        start += pattern.length();

        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length())
            return null;

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            if (end == -1)
                return null;
            return json.substring(start, end);
        } else {
            int end = start;
            while (end < json.length() && (Character.isLetterOrDigit(json.charAt(end)) || json.charAt(end) == '.')) {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
