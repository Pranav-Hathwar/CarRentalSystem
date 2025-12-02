package com.carrental.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
        // H2 Database URL
        private static final String URL = "jdbc:h2:~/car_rental_db;DB_CLOSE_DELAY=-1";
        private static final String USER = "sa";
        private static final String PASSWORD = "";

        static {
                try {
                        // Load H2 Driver
                        Class.forName("org.h2.Driver");
                        initDatabase();
                } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                }
        }

        public static Connection getConnection() throws SQLException {
                return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        private static void initDatabase() {
                System.out.println("DBConnection: Initializing Database at " + URL); // LOG
                String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "name VARCHAR(255), " +
                                "email VARCHAR(255) UNIQUE, " +
                                "password VARCHAR(255), " +
                                "role VARCHAR(50))";

                String createCarsTable = "CREATE TABLE IF NOT EXISTS cars (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "name VARCHAR(255), " +
                                "price DOUBLE, " +
                                "image TEXT, " +
                                "features TEXT)";

                String createBookingsTable = "CREATE TABLE IF NOT EXISTS bookings (" +
                                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                "user_id INT, " +
                                "car_id INT, " +
                                "start_date DATE, " +
                                "end_date DATE, " +
                                "total_price DOUBLE, " +
                                "status VARCHAR(50), " +
                                "payment_status VARCHAR(50) DEFAULT 'UNPAID', " +
                                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                                "FOREIGN KEY (car_id) REFERENCES cars(id))";

                try (Connection conn = getConnection();
                                Statement stmt = conn.createStatement()) {
                        stmt.execute(createUsersTable);
                        stmt.execute(createCarsTable);
                        stmt.execute(createBookingsTable);

                        // Ensure new columns for vehicle type and registration number exist (safe to
                        // run)
                        try {
                                stmt.execute("ALTER TABLE cars ADD COLUMN IF NOT EXISTS registration_number VARCHAR(100)");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE cars ADD COLUMN IF NOT EXISTS type VARCHAR(50) DEFAULT 'CAR'");
                        } catch (SQLException ignore) {
                        }
                        // Ensure bookings table has extended columns
                        try {
                                stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS pickup_datetime TIMESTAMP");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS dropoff_datetime TIMESTAMP");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS driving_license_path VARCHAR(1024)");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) DEFAULT 'UNPAID'");
                        } catch (SQLException ignore) {
                        }

                        // Migrate old column names if they exist
                        try {
                                stmt.execute("ALTER TABLE bookings ALTER COLUMN customer_id RENAME TO user_id");
                                System.out.println("DBConnection: Renamed customer_id to user_id");
                        } catch (SQLException ignore) {
                        }
                        try {
                                stmt.execute("ALTER TABLE bookings ALTER COLUMN vehicle_id RENAME TO car_id");
                                System.out.println("DBConnection: Renamed vehicle_id to car_id");
                        } catch (SQLException ignore) {
                        }

                        // Seed Data
                        java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                        if (rs.next() && rs.getInt(1) == 0) {
                                System.out.println("DBConnection: Seeding Users..."); // LOG
                                stmt.execute(
                                                "INSERT INTO users (name, email, password, role) VALUES ('Admin', 'admin@example.com', 'admin', 'ADMIN')");
                                stmt.execute(
                                                "INSERT INTO users (name, email, password, role) VALUES ('User', 'user@example.com', 'user', 'USER')");
                        } else {
                                System.out.println("DBConnection: Users table already populated."); // LOG
                        }

                        rs = stmt.executeQuery("SELECT COUNT(*) FROM cars");
                        if (rs.next() && rs.getInt(1) == 0) {
                                System.out.println("DBConnection: Seeding Cars..."); // LOG

                                // Economy Cars (₹ per day)
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Toyota Camry', 45.0, 'images/camry.jpg', 'GPS Navigation, Bluetooth, Automatic Transmission, Air Conditioning, Backup Camera')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Honda Civic', 42.0, 'images/civic.jpg', 'Backup Camera, Apple CarPlay, Android Auto, LED Headlights, Keyless Entry')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Chevrolet Malibu', 43.0, 'images/malibu.jpg', 'Wi-Fi Hotspot, Remote Start, Heated Seats, Leather Interior, Sunroof')");

                                // Luxury Cars
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Tesla Model 3', 85.0, 'images/tesla.jpg', 'Electric Vehicle, Autopilot, Premium Sound System, All-Wheel Drive, Panoramic Roof')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Ford Mustang', 75.0, 'images/mustang.jpg', 'V8 Engine, Sport Mode, Premium Audio, Leather Seats, Convertible Top')");

                                // Additional Premium Cars - use local images as fallback
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('BMW 3 Series', 95.0, 'images/default.jpg', 'Luxury Interior, Advanced Safety, Premium Sound, Navigation System, Sport Package')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Mercedes-Benz C-Class', 98.0, 'images/default.jpg', 'MBUX Infotainment, Ambient Lighting, Wireless Charging, Premium Package, Panoramic Sunroof')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Audi A4', 92.0, 'images/default.jpg', 'Quattro AWD, Virtual Cockpit, Bang & Olufsen Audio, Adaptive Cruise Control, Matrix LED')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Lexus ES', 90.0, 'images/default.jpg', 'Mark Levinson Audio, Premium Leather, Advanced Safety, Hybrid Engine, Climate Control')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Jaguar XE', 96.0, 'images/default.jpg', 'InControl Touch, Meridian Sound, Sport Mode, All-Wheel Drive, Premium Interior')");
                                stmt.execute(
                                                "INSERT INTO cars (name, price, image, features) VALUES ('Volvo S60', 88.0, 'images/default.jpg', 'Safety Features, Android Auto, Apple CarPlay, Premium Sound, Panoramic Roof')");

                                System.out.println("DBConnection: Cars seeded successfully!");
                        }

                        System.out.println("DBConnection: Database initialized successfully.");
                } catch (SQLException e) {
                        e.printStackTrace();
                }
        }
}
