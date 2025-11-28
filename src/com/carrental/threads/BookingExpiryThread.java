package com.carrental.threads;

import com.carrental.model.Booking;
import com.carrental.service.BookingService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class BookingExpiryThread extends Thread {
    /*
     * private BookingService bookingService;
     * private volatile boolean running = true;
     * private static final int EXPIRY_SECONDS = 30; // Short time for demo purposes
     * 
     * public BookingExpiryThread(BookingService bookingService) {
     * this.bookingService = bookingService;
     * }
     * 
     * @Override
     * public void run() {
     * // ...
     * }
     * 
     * public void stopThread() {
     * running = false;
     * }
     */
    public BookingExpiryThread(Object service) {
    } // Dummy constructor

    public void stopThread() {
    }
}
