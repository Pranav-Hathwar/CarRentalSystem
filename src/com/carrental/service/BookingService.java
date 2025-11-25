package com.carrental.service;

import com.carrental.interfaces.PaymentProcessor;
import com.carrental.model.Booking;
import com.carrental.model.Car;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BookingService {
    private Map<Integer, Booking> bookings;
    private AtomicInteger bookingIdCounter;

    public BookingService() {
        this.bookings = new HashMap<>();
        this.bookingIdCounter = new AtomicInteger(1);
    }

    public Booking createBooking(Car car, String customerName, int days) {
        if (!car.isAvailable()) {
            throw new IllegalStateException("Car is not available.");
        }
        int bookingId = bookingIdCounter.getAndIncrement();
        Booking booking = new Booking(bookingId, car, customerName, days);
        bookings.put(bookingId, booking);

        // Mark car as unavailable immediately upon booking creation (pending state)
        // In a real system, you might hold it for a short time, but for simplicity we
        // mark it rented.
        // If payment fails or times out, we'll release it.
        car.setAvailable(false);

        return booking;
    }

    public boolean confirmBooking(int bookingId, PaymentProcessor paymentProcessor) {
        Booking booking = bookings.get(bookingId);
        if (booking == null || booking.getStatus() != Booking.Status.PENDING) {
            return false;
        }

        boolean paymentSuccess = paymentProcessor.processPayment(booking.getTotalPrice());
        if (paymentSuccess) {
            booking.setStatus(Booking.Status.CONFIRMED);
            return true;
        } else {
            // If payment fails, we might want to cancel or keep pending.
            // For this demo, let's keep it pending and let the user try again or
            // auto-cancel.
            return false;
        }
    }

    public boolean cancelBooking(int bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            return false;
        }
        if (booking.getStatus() == Booking.Status.CANCELLED) {
            return false;
        }

        booking.setStatus(Booking.Status.CANCELLED);
        booking.getCar().setAvailable(true); // Release the car
        return true;
    }

    public Map<Integer, Booking> getBookings() {
        return bookings;
    }

    public Booking getBooking(int bookingId) {
        return bookings.get(bookingId);
    }
}
