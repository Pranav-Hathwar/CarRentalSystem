package com.carrental.servlet;

import com.carrental.dao.UserDAO;
import com.carrental.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        System.out.println("AuthServlet: Received request for path: " + path); // LOG
        setCorsHeaders(resp);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        if ("/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(path)) {
            handleLogout(req, resp);
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = readBody(req);
            System.out.println("AuthServlet: Register body: " + body); // LOG
            String name = extractJsonValue(body, "name");
            String email = extractJsonValue(body, "email");
            String password = extractJsonValue(body, "password");
            String role = extractJsonValue(body, "role");

            if (role == null || role.isEmpty())
                role = "USER";

            if (name == null || name.isEmpty() || email == null || email.isEmpty() || password == null
                    || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"All fields are required\"}");
                return;
            }

            User user = new User(0, name, email, password, role);
            if (userDAO.registerUser(user)) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true, \"message\":\"User registered successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"success\":false, \"message\":\"Email already exists\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false, \"message\":\"Registration error: " + e.getMessage() + "\"}");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = readBody(req);
            System.out.println("AuthServlet: Login body: " + body); // LOG
            String email = extractJsonValue(body, "email");
            String password = extractJsonValue(body, "password");

            System.out.println("AuthServlet: Login attempt for email: " + email); // LOG

            if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false, \"message\":\"Email and password are required\"}");
                return;
            }

            User user = userDAO.loginUser(email, password);
            if (user != null) {
                System.out.println("AuthServlet: Login successful for user: " + user.getName()); // LOG
                HttpSession session = req.getSession();
                session.setAttribute("user", user);

                resp.setStatus(HttpServletResponse.SC_OK);
                String username = escapeJson(user.getName());
                String userRole = escapeJson(user.getRole());
                resp.getWriter().write(String.format(
                        "{\"success\":true, \"id\":%d, \"username\":\"%s\", \"role\":\"%s\", \"email\":\"%s\", \"message\":\"Login successful\"}",
                        user.getId(), username, userRole, email));
            } else {
                System.out.println("AuthServlet: Login failed (invalid credentials)"); // LOG
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"success\":false, \"message\":\"Invalid email or password\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false, \"message\":\"Login error: " + e.getMessage() + "\"}");
        }
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"success\":true, \"message\":\"Logged out\"}");
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
        if (json == null || json.isEmpty())
            return null;

        String pattern = "\"" + key + "\"";
        int start = json.indexOf(pattern);
        if (start == -1)
            return null;

        start += pattern.length();
        while (start < json.length() && json.charAt(start) != ':') {
            if (json.charAt(start) == '"')
                return null; // Malformed
            start++;
        }
        if (start >= json.length())
            return null;
        start++; // Skip ':'

        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length())
            return null;

        // Check if string
        if (json.charAt(start) == '"') {
            start++;
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }
            if (end >= json.length())
                return null;
            String value = json.substring(start, end);
            return value.replace("\\\"", "\"").replace("\\\\", "\\");
        } else {
            // Number, boolean, or null
            int end = start;
            while (end < json.length() &&
                    (Character.isLetterOrDigit(json.charAt(end)) ||
                            json.charAt(end) == '.' || json.charAt(end) == '@' ||
                            json.charAt(end) == '-' || json.charAt(end) == '+')) {
                end++;
            }
            String value = json.substring(start, end).trim();
            if (value.equals("null"))
                return null;
            return value;
        }
    }
}
