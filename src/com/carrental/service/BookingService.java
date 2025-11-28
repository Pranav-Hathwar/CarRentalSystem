package com.carrental.service;

import com.carrental.model.Booking;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class BookingService {
    private List<Booking> bookings;
    private AtomicLong bookingIdCounter;

    public BookingService() {
        this.bookings = new ArrayList<>();
        this.bookingIdCounter = new AtomicLong(1);
    }

    public Booking createBooking(Booking booking) {
        booking.setId(bookingIdCounter.getAndIncrement());
        bookings.add(0, booking); // Add to top
        return booking;
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings);
    }
}
