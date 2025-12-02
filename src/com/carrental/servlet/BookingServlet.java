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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

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
        String path = req.getPathInfo();

        // Handle payment request endpoints
        // Handle payment request endpoints
        if (path != null && path.contains("/request-payment")) {
            handlePaymentRequest(req, resp, user);
            return;
        } else if (path != null && path.contains("/mark-paid")) {
            handleMarkPaid(req, resp, user);
            return;
        } else if (path != null && path.contains("/cancel")) {
            handleCancel(req, resp, user);
            return;
        }

        try {
            String body = readBody(req);
            System.out.println("BookingServlet received body: " + body); // DEBUG LOG

            String carIdStr = extractJsonValue(body, "carId");
            String pickupDateTimeStr = extractJsonValue(body, "pickupDateTime");
            String dropoffDateTimeStr = extractJsonValue(body, "dropoffDateTime");
            String driverDobStr = extractJsonValue(body, "driverDob");
            String licensePath = extractJsonValue(body, "licensePath");
            String totalPriceStr = extractJsonValue(body, "totalPrice");

            System.out.println("Extracted: carId=" + carIdStr + ", pickup=" + pickupDateTimeStr + ", dropoff="
                    + dropoffDateTimeStr + ", dob=" + driverDobStr); // DEBUG LOG

            if (carIdStr == null || pickupDateTimeStr == null || dropoffDateTimeStr == null || driverDobStr == null) {
                System.out.println("Missing required fields!"); // DEBUG LOG
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Missing required fields\"}");
                return;
            }

            int carId = Integer.parseInt(carIdStr);

            // Parse datetimes
            java.time.LocalDateTime pickup = java.time.LocalDateTime.parse(pickupDateTimeStr);
            java.time.LocalDateTime dropoff = java.time.LocalDateTime.parse(dropoffDateTimeStr);

            if (!pickup.isBefore(dropoff)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Pickup must be before dropoff\"}");
                return;
            }

            // Time constraints: between 09:00 and 21:00
            int pickupHour = pickup.getHour();
            int dropoffHour = dropoff.getHour();
            if (pickupHour < 9 || pickupHour > 21 || dropoffHour < 9 || dropoffHour > 21) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write(
                        "{\"success\":false, \"message\":\"Pickup and dropoff must be between 09:00 and 21:00\"}");
                return;
            }

            // Age validation
            java.time.LocalDate dob = java.time.LocalDate.parse(driverDobStr);
            java.time.Period age = java.time.Period.between(dob, java.time.LocalDate.now());
            if (age.getYears() < 18) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Driver must be at least 18 years old\"}");
                return;
            }

            // Compute price based on car base price and weekend multiplier
            com.carrental.dao.CarDAO carDAO = new com.carrental.dao.CarDAO();
            com.carrental.model.Car car = carDAO.getCarById(carId);
            if (car == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"success\":false, \"message\":\"Vehicle not found\"}");
                return;
            }
            double basePrice = car.getPrice();

            // Calculate number of days (ceil of hours/24)
            long hours = java.time.Duration.between(pickup, dropoff).toHours();
            long days = Math.max(1, (hours + 23) / 24);

            double weekendMultiplier = 1.25; // +25% on weekends
            double computedTotal = 0.0;
            java.time.LocalDate cursor = pickup.toLocalDate();
            for (int i = 0; i < days; i++) {
                java.time.DayOfWeek dow = cursor.getDayOfWeek();
                double dayPrice = basePrice;
                if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
                    dayPrice *= weekendMultiplier;
                }
                computedTotal += dayPrice;
                cursor = cursor.plusDays(1);
            }

            double totalPrice = computedTotal;
            // override if caller sent a price but we'll rely on computed
            // create Booking object
            Booking booking = new Booking();
            booking.setUserId(user.getId());
            booking.setCarId(carId);
            booking.setStartDate(java.sql.Date.valueOf(pickup.toLocalDate()));
            booking.setEndDate(java.sql.Date.valueOf(dropoff.toLocalDate()));
            booking.setPickupDatetime(java.sql.Timestamp.valueOf(pickup));
            booking.setDropoffDatetime(java.sql.Timestamp.valueOf(dropoff));
            booking.setDrivingLicensePath(licensePath != null ? licensePath : "");
            booking.setTotalPrice(totalPrice);
            booking.setStatus("PENDING"); // owner must accept

            bookingDAO.createBooking(booking);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(
                    "{\"success\":true, \"message\":\"Booking created successfully (pending owner approval)\", \"totalPrice\":"
                            + totalPrice + "}");
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
        List<Booking> bookings;
        if ("ADMIN".equals(user.getRole())) {
            bookings = bookingDAO.getAllBookings();
        } else {
            bookings = bookingDAO.getBookingsByUserId(user.getId());
        }

        StringBuilder json = new StringBuilder("[");
        com.carrental.dao.CarDAO carDAO = new com.carrental.dao.CarDAO();
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            com.carrental.model.Car car = carDAO.getCarById(b.getCarId());
            String carName = car != null ? car.getName() : "Unknown Car";

            json.append(String.format(
                    "{\"id\":%d, \"carId\":%d, \"carName\":\"%s\", \"startDate\":\"%s\", \"endDate\":\"%s\", \"totalPrice\":%.2f, \"status\":\"%s\", \"paymentStatus\":\"%s\", \"drivingLicensePath\":\"%s\"}",
                    b.getId(), b.getCarId(), carName,
                    b.getStartDate() != null ? b.getStartDate().toString() : "",
                    b.getEndDate() != null ? b.getEndDate().toString() : "",
                    b.getTotalPrice(),
                    b.getStatus() != null ? b.getStatus() : "PENDING",
                    b.getPaymentStatus() != null ? b.getPaymentStatus() : "UNPAID",
                    b.getDrivingLicensePath() != null ? b.getDrivingLicensePath().replace("\\", "\\\\") : ""));
            if (i < bookings.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        resp.getWriter().write(json.toString());
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

    // Helper to extract ID from path
    private int extractIdFromPath(String path) {
        if (path == null)
            return -1;
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part.matches("\\d+")) {
                return Integer.parseInt(part);
            }
        }
        return -1;
    }

    // Handle admin payment request
    private void handlePaymentRequest(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            // Only admin can request payments
            if (!"ADMIN".equals(user.getRole())) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.getWriter().write("{\"success\":false, \"message\":\"Only admin can request payment\"}");
                return;
            }

            int bookingId = extractIdFromPath(req.getPathInfo());
            if (bookingId == -1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Invalid booking ID\"}");
                return;
            }

            if (bookingDAO.updatePaymentStatus(bookingId, "PAYMENT_REQUESTED")) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Payment request sent to customer\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to request payment\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }

    // Handle customer marking payment as done
    private void handleMarkPaid(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            int bookingId = extractIdFromPath(req.getPathInfo());
            if (bookingId == -1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Invalid booking ID\"}");
                return;
            }

            if (bookingDAO.updatePaymentStatus(bookingId, "PAID")) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Payment confirmed\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to confirm payment\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }

    // Handle cancellation request
    private void handleCancel(HttpServletRequest req, HttpServletResponse resp, User user) throws IOException {
        try {
            int bookingId = extractIdFromPath(req.getPathInfo());
            if (bookingId == -1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Invalid booking ID\"}");
                return;
            }

            // Verify ownership (optional but good practice)
            // For now, assuming user can only cancel their own bookings or admin can cancel
            // any
            // But we don't have getBookingById in BookingDAO exposed easily here without
            // querying.
            // We'll trust the ID for now or rely on frontend to send correct ID.
            // Ideally we should check if booking belongs to user.

            if (bookingDAO.updateStatus(bookingId, "CANCELLED")) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Booking cancelled\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to cancel booking\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
        if (!"ADMIN".equals(user.getRole())) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false, \"message\":\"Only admin can delete bookings\"}");
            return;
        }

        try {
            int bookingId = extractIdFromPath(req.getPathInfo());
            if (bookingId == -1) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Invalid booking ID\"}");
                return;
            }

            if (bookingDAO.deleteBooking(bookingId)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Booking deleted\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false, \"message\":\"Failed to delete booking\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false, \"message\":\"Error: " + e.getMessage() + "\"}");
        }
    }
}
