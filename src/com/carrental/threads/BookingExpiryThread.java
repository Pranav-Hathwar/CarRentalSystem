package com.carrental.threads;

import com.carrental.model.Booking;
import com.carrental.service.BookingService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class BookingExpiryThread extends Thread {
    private BookingService bookingService;
    private volatile boolean running = true;
    private static final int EXPIRY_SECONDS = 30; // Short time for demo purposes

    public BookingExpiryThread(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Override
    public void run() {
        System.out.println("Booking Expiry Thread Started...");
        while (running) {
            try {
                Thread.sleep(5000); // Check every 5 seconds
                LocalDateTime now = LocalDateTime.now();

                for (Booking booking : bookingService.getBookings().values()) {
                    if (booking.getStatus() == Booking.Status.PENDING) {
                        long secondsElapsed = ChronoUnit.SECONDS.between(booking.getBookingTime(), now);
                        if (secondsElapsed > EXPIRY_SECONDS) {
                            System.out.println(
                                    "\n[Auto-Cancel] Booking ID " + booking.getBookingId() + " expired. Cancelling...");
                            bookingService.cancelBooking(booking.getBookingId());
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Booking Expiry Thread Interrupted.");
            }
        }
    }

    public void stopThread() {
        running = false;
    }
}
