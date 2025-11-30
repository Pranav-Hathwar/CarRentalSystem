package com.carrental.servlet;

import com.carrental.dao.BookingDAO;
import com.carrental.model.Booking;
import com.carrental.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/api/bookings/*")
public class BookingServlet extends HttpServlet {
    private BookingDAO bookingDAO = new BookingDAO();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Check authentication
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false, \"message\":\"Unauthorized\"}");
            return;
        }

        User user = (User) session.getAttribute("user");

        try {
            String body = readBody(req);
            String carIdStr = extractJsonValue(body, "carId");
            String startDateStr = extractJsonValue(body, "startDate");
            String endDateStr = extractJsonValue(body, "endDate");
            String totalPriceStr = extractJsonValue(body, "totalPrice");
            
            if (carIdStr == null || startDateStr == null || endDateStr == null || totalPriceStr == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Missing required fields\"}");
                return;
            }
            
            int carId = Integer.parseInt(carIdStr);
            double totalPrice = Double.parseDouble(totalPriceStr);

            Booking booking = new Booking();
            booking.setUserId(user.getId());
            booking.setCarId(carId);
            booking.setStartDate(Date.valueOf(startDateStr));
            booking.setEndDate(Date.valueOf(endDateStr));
            booking.setTotalPrice(totalPrice);
            booking.setStatus("CONFIRMED");

            if (bookingDAO.createBooking(booking)) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true, \"message\":\"Booking created successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to create booking\"}");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid date or number format\"}");
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Invalid request data: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // Check authentication
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false, \"message\":\"Unauthorized\"}");
            return;
        }

        User user = (User) session.getAttribute("user");
        List<Booking> bookings = bookingDAO.getBookingsByUserId(user.getId());

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            json.append(String.format(
                    "{\"id\":%d, \"carId\":%d, \"startDate\":\"%s\", \"endDate\":\"%s\", \"totalPrice\":%.2f, \"status\":\"%s\"}",
                    b.getId(), b.getCarId(), 
                    b.getStartDate() != null ? b.getStartDate().toString() : "",
                    b.getEndDate() != null ? b.getEndDate().toString() : "",
                    b.getTotalPrice(), 
                    b.getStatus() != null ? b.getStatus() : "PENDING"));
            if (i < bookings.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        resp.getWriter().write(json.toString());
    }
    
    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;
        start += pattern.length();

        // Check if value is string (starts with quote)
        char firstChar = json.charAt(start);
        while (Character.isWhitespace(firstChar)) {
            start++;
            firstChar = json.charAt(start);
        }

        if (firstChar == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            // Number or boolean
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
                end++;
            }
            // Handle comma or closing brace
            int comma = json.indexOf(",", start);
            int brace = json.indexOf("}", start);
            if (comma != -1 && comma < brace)
                end = comma;
            else if (brace != -1)
                end = brace;

            return json.substring(start, end).trim();
        }
    }
}
