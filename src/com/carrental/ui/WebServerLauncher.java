package com.carrental.ui;

import com.carrental.service.BookingService;
import com.carrental.service.VehicleService;

import java.io.IOException;

public class WebServerLauncher {
    public static void main(String[] args) {
        try {
            VehicleService vehicleService = new VehicleService();
            BookingService bookingService = new BookingService();
            SimpleWebServer server = new SimpleWebServer(vehicleService, bookingService);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
