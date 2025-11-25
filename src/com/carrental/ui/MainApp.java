package com.carrental.ui;

import com.carrental.interfaces.PaymentProcessor;
import com.carrental.model.Booking;
import com.carrental.model.Car;
import com.carrental.service.BookingService;
import com.carrental.service.VehicleService;
import com.carrental.threads.BookingExpiryThread;

import java.util.List;
import java.util.Scanner;

public class MainApp {
    private static VehicleService vehicleService = new VehicleService();
    private static BookingService bookingService = new BookingService();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Start the auto-cancellation thread
        BookingExpiryThread expiryThread = new BookingExpiryThread(bookingService);
        expiryThread.start();

        // Simple payment processor implementation (always succeeds for demo)
        PaymentProcessor paymentProcessor = amount -> {
            System.out.println("Processing payment of $" + amount + "...");
            return true;
        };

        boolean exit = false;
        while (!exit) {
            printMenu();
            int choice = -1;
            try {
                String input = scanner.nextLine();
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1:
                    listAvailableCars();
                    break;
                case 2:
                    rentCar();
                    break;
                case 3:
                    confirmBooking(paymentProcessor);
                    break;
                case 4:
                    cancelBooking();
                    break;
                case 5:
                    viewMyBookings();
                    break;
                case 6:
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        System.out.println("Exiting system...");
        expiryThread.stopThread();
        // Wait a bit for the thread to stop gracefully if needed, or just exit
        System.exit(0);
    }

    private static void printMenu() {
        System.out.println("\n--- Car Rental System ---");
        System.out.println("1. List Available Cars");
        System.out.println("2. Rent a Car");
        System.out.println("3. Confirm Booking (Pay)");
        System.out.println("4. Cancel Booking");
        System.out.println("5. View All Bookings");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void listAvailableCars() {
        System.out.println("\n--- Available Cars ---");
        List<Car> cars = vehicleService.getAvailableCars();
        if (cars.isEmpty()) {
            System.out.println("No cars available at the moment.");
        } else {
            for (Car car : cars) {
                System.out.println(car);
            }
        }
    }

    private static void rentCar() {
        System.out.println("\n--- Rent a Car ---");
        System.out.print("Enter Car ID to rent: ");
        try {
            int carId = Integer.parseInt(scanner.nextLine());
            Car car = vehicleService.findCarById(carId).orElse(null);

            if (car == null) {
                System.out.println("Car not found.");
                return;
            }
            if (!car.isAvailable()) {
                System.out.println("Car is not available.");
                return;
            }

            System.out.print("Enter your name: ");
            String name = scanner.nextLine();

            System.out.print("Enter number of days: ");
            int days = Integer.parseInt(scanner.nextLine());

            Booking booking = bookingService.createBooking(car, name, days);
            System.out.println("Booking created successfully!");
            System.out.println(booking);
            System.out.println("PLEASE NOTE: You must confirm (pay) within 30 seconds or it will be auto-cancelled.");

        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void confirmBooking(PaymentProcessor paymentProcessor) {
        System.out.println("\n--- Confirm Booking ---");
        System.out.print("Enter Booking ID: ");
        try {
            int bookingId = Integer.parseInt(scanner.nextLine());
            Booking booking = bookingService.getBooking(bookingId);

            if (booking == null) {
                System.out.println("Booking not found.");
                return;
            }

            if (booking.getStatus() != Booking.Status.PENDING) {
                System.out.println("Booking is in " + booking.getStatus() + " state. Cannot confirm.");
                return;
            }

            boolean success = bookingService.confirmBooking(bookingId, paymentProcessor);
            if (success) {
                System.out.println("Payment successful! Booking confirmed.");
            } else {
                System.out.println("Payment failed.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    private static void cancelBooking() {
        System.out.println("\n--- Cancel Booking ---");
        System.out.print("Enter Booking ID: ");
        try {
            int bookingId = Integer.parseInt(scanner.nextLine());
            boolean success = bookingService.cancelBooking(bookingId);
            if (success) {
                System.out.println("Booking cancelled successfully.");
            } else {
                System.out.println("Could not cancel booking (invalid ID or already cancelled).");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    private static void viewMyBookings() {
        System.out.println("\n--- All Bookings ---");
        var bookings = bookingService.getBookings();
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
        } else {
            bookings.values().forEach(System.out::println);
        }
    }
}
