package com.carrental.model;

import java.time.LocalDateTime;

public class Booking {
    public enum Status {
        PENDING, CONFIRMED, CANCELLED
    }

    private int bookingId;
    private Car car;
    private String customerName;
    private int days;
    private double totalPrice;
    private Status status;
    private LocalDateTime bookingTime;

    public Booking(int bookingId, Car car, String customerName, int days) {
        this.bookingId = bookingId;
        this.car = car;
        this.customerName = customerName;
        this.days = days;
        this.totalPrice = car.calculatePrice(days);
        this.status = Status.PENDING;
        this.bookingTime = LocalDateTime.now();
    }

    public int getBookingId() {
        return bookingId;
    }

    public Car getCar() {
        return car;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getDays() {
        return days;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    @Override
    public String toString() {
        return "Booking ID: " + bookingId +
                " | Customer: " + customerName +
                " | Car: " + car.getMake() + " " + car.getModel() +
                " | Days: " + days +
                " | Total: $" + totalPrice +
                " | Status: " + status;
    }
}
