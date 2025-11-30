package com.carrental.dao;

import com.carrental.model.Booking;
import com.carrental.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    public boolean createBooking(Booking booking) {
        String sql = "INSERT INTO bookings (user_id, car_id, start_date, end_date, total_price, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getUserId());
            pstmt.setInt(2, booking.getCarId());
            pstmt.setDate(3, booking.getStartDate());
            pstmt.setDate(4, booking.getEndDate());
            pstmt.setDouble(5, booking.getTotalPrice());
            pstmt.setString(6, booking.getStatus());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE user_id = ?";
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
                        rs.getDouble("total_price"),
                        rs.getString("status"));
                bookings.add(booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }
}
