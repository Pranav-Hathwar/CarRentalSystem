package com.carrental.servlet;

import com.carrental.dao.CarDAO;
import com.carrental.model.Car;
import com.carrental.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/admin/cars")
public class AdminCarServlet extends HttpServlet {
    private CarDAO carDAO = new CarDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Check if user is authenticated and has admin role
        HttpSession session = req.getSession(false);
        User user = null;
        if (session != null) {
            user = (User) session.getAttribute("user");
        }

        if (user == null || !user.getRole().equals("ADMIN")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false, \"message\":\"Admin access required\"}");
            return;
        }

        // Read request body
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();

        // Parse JSON
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

        // Set defaults
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
            double price = Double.parseDouble(priceStr);
            Car car = new Car(0, name, price, image, features, type, registrationNumber);
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

}
