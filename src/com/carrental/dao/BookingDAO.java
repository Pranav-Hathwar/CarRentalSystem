package com.carrental.dao;

import com.carrental.model.Booking;
import com.carrental.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public void createBooking(Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, car_id, start_date, end_date, pickup_datetime, dropoff_datetime, driving_license_path, total_price, status, payment_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getUserId());
            pstmt.setInt(2, booking.getCarId());
            pstmt.setDate(3, booking.getStartDate());
            pstmt.setDate(4, booking.getEndDate());
            pstmt.setTimestamp(5, booking.getPickupDatetime());
            pstmt.setTimestamp(6, booking.getDropoffDatetime());
            pstmt.setString(7, booking.getDrivingLicensePath());
            pstmt.setDouble(8, booking.getTotalPrice());
            pstmt.setString(9, booking.getStatus());
            pstmt.setString(10, booking.getPaymentStatus() != null ? booking.getPaymentStatus() : "UNPAID");

            pstmt.executeUpdate();
        }
    }

    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Booking booking = new Booking(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("car_id"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getTimestamp("pickup_datetime"),
                        rs.getTimestamp("dropoff_datetime"),
                        rs.getDouble("total_price"),
                        rs.getString("status"),
                        rs.getString("driving_license_path"),
                        rs.getTimestamp("created_at"));
                booking.setPaymentStatus(
                        rs.getString("payment_status") != null ? rs.getString("payment_status") : "UNPAID");
                bookings.add(booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Booking booking = new Booking(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("car_id"),
                        rs.getDate("start_date"),
                        rs.getDate("end_date"),
                        rs.getTimestamp("pickup_datetime"),
                        rs.getTimestamp("dropoff_datetime"),
                        rs.getDouble("total_price"),
                        rs.getString("status"),
                        rs.getString("driving_license_path"),
                        rs.getTimestamp("created_at"));
                booking.setPaymentStatus(
                        rs.getString("payment_status") != null ? rs.getString("payment_status") : "UNPAID");
                bookings.add(booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    public boolean updatePaymentStatus(int bookingId, String paymentStatus) {
        String sql = "UPDATE bookings SET payment_status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paymentStatus);
            pstmt.setInt(2, bookingId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStatus(int bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, bookingId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
