package com.carrental.dao;

import com.carrental.model.Car;
import com.carrental.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarDAO {

    public List<Car> getAllCars() {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT * FROM cars";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cars.add(new Car(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getString("image"),
                        rs.getString("features")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cars;
    }

    public boolean addCar(Car car) {
        String sql = "INSERT INTO cars (name, price, image, features) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, car.getName());
            pstmt.setDouble(2, car.getPrice());
            pstmt.setString(3, car.getImage());
            pstmt.setString(4, car.getFeatures());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteCar(int id) {
        String sql = "DELETE FROM cars WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
